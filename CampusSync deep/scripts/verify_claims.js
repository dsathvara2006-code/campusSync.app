// verify_claims.js
// Ye script check karti hai ki kisi specific user par 
// custom claims (role, college_id) actually set hain ya nahi

const { initializeApp, cert } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const serviceAccount = require("./serviceAccountKey.json");

initializeApp({ credential: cert(serviceAccount) });

async function verifyClaims(uid) {
  try {
    const user = await getAuth().getUser(uid);
    console.log("User Email:", user.email);
    console.log("Custom Claims:", user.customClaims);

    if (!user.customClaims || !user.customClaims.role) {
      console.log("\n❌ PROBLEM: Is user par koi claims set nahi hain!");
      console.log("Fix: node set_principal_claims.js", uid, "<collegeId>");
    } else {
      console.log("\n✅ Claims sahi se set hain.");
    }
  } catch (error) {
    console.error("Error - shayad ye UID exist nahi karta:", error.message);
  }
}

// Usage: node verify_claims.js <uid>
const uid = process.argv[2];
if (!uid) {
  console.error("Usage: node verify_claims.js <uid>");
  process.exit(1);
}
verifyClaims(uid);
