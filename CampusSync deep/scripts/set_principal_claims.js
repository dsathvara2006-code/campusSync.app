// Ye script ek baar developer apne local machine par chalata hai
// jab naya college onboard karna ho. Kabhi bhi app/client code se
// automatically call NAHI hoti.

const { initializeApp, cert } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const serviceAccount = require("./serviceAccountKey.json");

initializeApp({ credential: cert(serviceAccount) });

async function setPrincipalClaims(uid, collegeId) {
  await getAuth().setCustomUserClaims(uid, {
    role: "principal",
    college_id: collegeId
  });
  console.log(`Claims set for ${uid} -> principal @ ${collegeId}`);
}

// Usage: node set_principal_claims.js <uid> <collegeId>
const [uid, collegeId] = process.argv.slice(2);
if (!uid || !collegeId) {
  console.error("Usage: node set_principal_claims.js <uid> <collegeId>");
  process.exit(1);
}
setPrincipalClaims(uid, collegeId);
