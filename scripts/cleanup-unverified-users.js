const { initializeApp, cert } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');

const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);

initializeApp({
  credential: cert(serviceAccount)
});

const auth = getAuth();

const GRACE_PERIOD_MS = 7 * 24 * 60 * 60 * 1000; // удаление пользвателя есть не подтвердил за неделю
const now = Date.now();
let nextPageToken;
let totalDeleted = 0;

async function cleanup() {
  do {
    const listUsersResult = await auth.listUsers(1000, nextPageToken);
    nextPageToken = listUsersResult.pageToken;

    const toDelete = listUsersResult.users
      .filter(user =>
        user.email &&
        !user.emailVerified &&
        user.metadata.creationTime &&
        (now - new Date(user.metadata.creationTime).getTime()) > GRACE_PERIOD_MS
      )
      .map(user => user.uid);

    if (toDelete.length > 0) {
      const deleteResult = await auth.deleteUsers(toDelete);
      totalDeleted += deleteResult.successCount;
      console.log(`Удалено ${deleteResult.successCount} пользователей.`);
    }
  } while (nextPageToken);

  console.log(`Всего удалено: ${totalDeleted}`);
}

cleanup().catch(console.error);