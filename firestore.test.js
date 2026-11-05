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

test("Unauthenticated user: cannot read projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("projects").get());
});

test("Authenticated user: cannot read another user's projects", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).collection("projects").doc("proj_1").set({
      id: "proj_1",
      title: "Bob's Project",
      userId: BOB_UID,
      aspectRatio: "9:16",
      durationMs: 5000,
      captionsJson: "[]",
      styleJson: "{}"
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("projects").doc("proj_1").get());
});

test("Authenticated user: can create and read their own project", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceProjRef = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_alice");

  await assertSucceeds(aliceProjRef.set({
    id: "proj_alice",
    title: "Alice's Awesome Reel",
    userId: ALICE_UID,
    aspectRatio: "9:16",
    durationMs: 15000,
    captionsJson: "[]",
    styleJson: "{}"
  }));

  await assertSucceeds(aliceProjRef.get());
});
