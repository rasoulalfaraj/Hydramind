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

function buildValidProfile(uid, ts) {
  return {
    userId: uid,
    name: "أحمد العراقي",
    age: 29,
    weightKg: 75,
    heightCm: 176,
    biologicalSex: "MALE",
    activityLevel: "MODERATE",
    workEnvironment: "INDOOR",
    outdoorMinutes: 45,
    exerciseMinutes: 30,
    sweatingLevel: "MODERATE",
    wakeHour: 7,
    bedHour: 23,
    medicalRestrictionType: "NONE",
    prescribedLimitMl: 0,
    iraqHeatProfile: "GENERAL_SUMMER",
    selectedCity: "Baghdad",
    languageCode: "ar",
    aiDialect: "MSA",
    habitLearningEnabled: true,
    reminderIntervalMinutes: 60,
    remindersEnabled: true,
    onboardingCompleted: true,
    streakDays: 3,
    createdAt: ts,
    updatedAt: ts,
  };
}

function buildValidLog(uid, ts) {
  return {
    userId: uid,
    beverageType: "WATER",
    amountMl: 250,
    effectiveHydrationMl: 250,
    dateKey: "2026-11-07",
    hourOfDay: 11,
    loggedAtMillis: 1794106420000,
    note: "كوب ماء بارد",
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated user cannot read or write profiles or logs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("logs").get());
});

test("Authenticated owner can create and read their own profile and logs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(buildValidProfile(ALICE_UID, now))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("logs").doc("log_1").set(buildValidLog(ALICE_UID, now))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("logs").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user cannot read or write another user's profile or logs", async () => {
  const now = new Date(Date.now() - 1000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(BOB_UID).set(buildValidProfile(BOB_UID, now));
    await db.collection("users").doc(BOB_UID).collection("logs").doc("bob_log").set(buildValidLog(BOB_UID, now));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("logs").doc("bob_log").get());
});

test("Shadow update with ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...buildValidProfile(ALICE_UID, now),
      isAdmin: true,
    })
  );
});
