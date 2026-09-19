const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

// 1. Notify user when their access request is approved or rejected
exports.onUserStatusUpdate = functions.firestore
  .document("users/{uid}")
  .onUpdate(async (change, context) => {
    const beforeData = change.before.data();
    const afterData = change.after.data();

    if (beforeData.status === "pending" && afterData.status !== "pending") {
      const payload = {
        notification: {
          title: "Access Request Update",
          body: afterData.status === "approved" 
            ? "Your request to join CampusSync has been approved!" 
            : `Your request was rejected. Reason: ${afterData.rejectionReason || 'No reason provided.'}`
        }
      };
      
      // In a real app, you would fetch the FCM token from a `fcmTokens` collection.
      // This is the skeletal logic for when you connect the Android App's FCM.
      console.log(`Sending notification to user ${context.params.uid}: ${payload.notification.body}`);
      // await admin.messaging().sendToDevice(fcmToken, payload);
    }
  });

// 2. Notify student when their fee payment is verified
exports.onFeeVerified = functions.firestore
  .document("colleges/{collegeId}/fee_payments/{paymentId}")
  .onUpdate(async (change, context) => {
    const beforeData = change.before.data();
    const afterData = change.after.data();

    if (beforeData.status === "submitted" && afterData.status === "approved") {
      const payload = {
        notification: {
          title: "Fee Payment Verified",
          body: `Your payment of ₹${afterData.amount} has been successfully verified.`
        }
      };
      
      console.log(`Sending fee notification to student ${afterData.studentId}`);
      // await admin.messaging().sendToDevice(fcmToken, payload);
    }
  });

// 3. Notify Admin when a new access request comes in
exports.onNewAccessRequest = functions.firestore
  .document("users/{uid}")
  .onCreate(async (snap, context) => {
    const userData = snap.data();
    if (userData.status === "pending" && userData.collegeId) {
      const payload = {
        notification: {
          title: "New Access Request",
          body: `${userData.name} has requested access as a ${userData.role}.`
        }
      };
      
      // In a real app, you would query admins for this collegeId and send them notifications
      console.log(`Sending notification to admins of college ${userData.collegeId}`);
    }
  });

// 4. Cleanup Pending Requests after 2 Days
exports.cleanupPendingRequests = functions.pubsub.schedule('every 24 hours').onRun(async (context) => {
  const twoDaysAgo = Date.now() - (2 * 24 * 60 * 60 * 1000);
  console.log(`Starting cleanup for requests older than ${twoDaysAgo}...`);
  
  const snapshot = await admin.firestore().collection("users")
    .where("status", "==", "pending")
    .where("createdAt", "<=", twoDaysAgo)
    .get();

  if (snapshot.empty) {
    console.log("No old pending requests found.");
    return null;
  }

  const batch = admin.firestore().batch();
  let deletedCount = 0;

  snapshot.forEach(doc => {
    batch.delete(doc.ref);
    // Note: To completely cleanup subcollections, you would need to also delete the document from 
    // `colleges/{collegeId}/{role}s/{uid}` but we skip that in this simple cleanup or handle it client-side
    // or via a separate Cloud Function.
    deletedCount++;
  });

  await batch.commit();
  console.log(`Successfully deleted ${deletedCount} old pending requests.`);
  return null;
});
