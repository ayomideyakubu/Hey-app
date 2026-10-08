const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read posts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("posts").get());
});

test("Authenticated user: can read posts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("posts").get());
});

test("Authenticated user: can create their own post", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const postRef = aliceDb.collection("posts").doc("post_1");
  await assertSucceeds(
    postRef.set({
      id: "post_1",
      authorId: ALICE_UID,
      authorUsername: "alice",
      authorDisplayName: "Alice Smith",
      content: "Hello hey world!",
      imageUrl: "https://example.com/photo.jpg",
      likesCount: 0,
      commentsCount: 0,
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot create post with spoofed authorId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const postRef = aliceDb.collection("posts").doc("post_spoof");
  await assertFails(
    postRef.set({
      id: "post_spoof",
      authorId: BOB_UID,
      authorUsername: "bob",
      authorDisplayName: "Bob Smith",
      content: "Impersonated post",
      likesCount: 0,
      commentsCount: 0,
      createdAt: new Date(),
    })
  );
});

test("User can like a post with own UID, cannot like with another UID", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const likeRef = aliceDb.collection("posts").doc("post_1").collection("likes").doc(ALICE_UID);
  await assertSucceeds(
    likeRef.set({
      userId: ALICE_UID,
      postId: "post_1",
      createdAt: new Date(),
    })
  );

  const bobLikeAsAlice = aliceDb.collection("posts").doc("post_1").collection("likes").doc(BOB_UID);
  await assertFails(
    bobLikeAsAlice.set({
      userId: BOB_UID,
      postId: "post_1",
      createdAt: new Date(),
    })
  );
});

test("Direct message: participant can read, non-participant cannot read", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("messages").doc("msg_1").set({
      id: "msg_1",
      senderId: ALICE_UID,
      senderName: "Alice",
      receiverId: BOB_UID,
      text: "Hey Bob!",
      participants: [ALICE_UID, BOB_UID],
      createdAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("messages").doc("msg_1").get());

  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();
  await assertFails(charlieDb.collection("messages").doc("msg_1").get());
});

test("Follow: user can follow another user with own UID, cannot follow as another user", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const followRef = aliceDb.collection("follows").doc(`${ALICE_UID}_${BOB_UID}`);
  await assertSucceeds(
    followRef.set({
      id: `${ALICE_UID}_${BOB_UID}`,
      followerId: ALICE_UID,
      targetUserId: BOB_UID,
      createdAt: new Date(),
    })
  );

  const bobSpoofFollow = aliceDb.collection("follows").doc(`${BOB_UID}_${ALICE_UID}`);
  await assertFails(
    bobSpoofFollow.set({
      id: `${BOB_UID}_${ALICE_UID}`,
      followerId: BOB_UID,
      targetUserId: ALICE_UID,
      createdAt: new Date(),
    })
  );
});

test("Notifications: actor can create notification for recipient, only recipient can read", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const notifRef = aliceDb.collection("users").doc(BOB_UID).collection("notifications").doc("notif_1");
  await assertSucceeds(
    notifRef.set({
      id: "notif_1",
      recipientId: BOB_UID,
      actorId: ALICE_UID,
      actorUsername: "alice",
      actorDisplayName: "Alice",
      type: "LIKE",
      postId: "post_1",
      read: false,
      createdAt: new Date(),
    })
  );

  // Bob can read his notifications
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("users").doc(BOB_UID).collection("notifications").doc("notif_1").get());

  // Alice cannot read Bob's notifications
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("notifications").doc("notif_1").get());
});

