const { initializeApp, cert } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const cloudinary = require('cloudinary').v2;

const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
initializeApp({ credential: cert(serviceAccount) });

const db = getFirestore();

cloudinary.config({ secure: true });
const PREFIX = 'socialnet/';
const PAGE_SIZE = 500;
const DELETE_BATCH_SIZE = 100;

// сбор изображений из Firestore
async function collectUsedIds() {
  const usedIds = new Set();

  console.log('Collecting public_ids from Firestore...');

  const postsSnapshot = await db.collection('posts').get();
  postsSnapshot.forEach(doc => {
    const data = doc.data();
    if (Array.isArray(data.imagePublicIds)) {
      data.imagePublicIds.forEach(id => { if (id) usedIds.add(id); });
    }
  });

  const usersSnapshot = await db.collection('users').get();
  usersSnapshot.forEach(doc => {
    const data = doc.data();
    if (data.avatarPublicId) usedIds.add(data.avatarPublicId);
  });

  console.log(`Collected ${usedIds.size} used public_ids.`);
  return usedIds;
}

// сбор всех изображений Cloudinary
async function collectAllCloudinaryIds() {
  const allIds = [];
  let nextCursor = null;
  let pageNumber = 0;
  let totalFetched = 0;

  do {
    pageNumber++;
    const response = await cloudinary.api.resources({
      type: 'upload',
      prefix: PREFIX,
      max_results: PAGE_SIZE,
      next_cursor: nextCursor,
    });

    const pageIds = (response.resources || []).map(r => r.public_id);
    allIds.push(...pageIds);
    totalFetched += pageIds.length;
    nextCursor = response.next_cursor || null;

    console.log(
      `Page ${pageNumber}: fetched ${pageIds.length} resources ` +
      `(total: ${totalFetched}${nextCursor ? ', has more' : ', done'})`
    );
  } while (nextCursor);

  return allIds;
}

// служебная функция чистки батчей
async function deleteInBatches(ids) {
  let totalDeleted = 0;
  let totalFailed = 0;

  for (let i = 0; i < ids.length; i += DELETE_BATCH_SIZE) {
    const batch = ids.slice(i, i + DELETE_BATCH_SIZE);
    try {
      const result = await cloudinary.api.delete_resources(batch);
      const deleted = result.deleted || {};
      const ok = Object.values(deleted).filter(v => v === 'deleted').length;
      const notFound = Object.values(deleted).filter(v => v === 'not_found').length;

      totalDeleted += ok;
      totalFailed += Object.keys(deleted).length - ok - notFound;

      console.log(
        `Batch ${Math.floor(i / DELETE_BATCH_SIZE) + 1}: ` +
        `deleted=${ok}, not_found=${notFound}`
      );
    } catch (e) {
      console.error(`Batch failed: ${e.message}`);
      totalFailed += batch.length;
    }
  }

  return { totalDeleted, totalFailed };
}

// главная функция очистки
async function cleanup() {
  console.log('Starting orphaned images cleanup...');

  const usedIds = await collectUsedIds();
  const cloudinaryIds = await collectAllCloudinaryIds();
  console.log(`Total in Cloudinary: ${cloudinaryIds.length}`);

  const orphans = cloudinaryIds.filter(id => !usedIds.has(id));
  console.log(`Found ${orphans.length} orphaned images.`);

  if (orphans.length === 0) {
    console.log('Nothing to delete.');
    return;
  }

  const { totalDeleted, totalFailed } = await deleteInBatches(orphans);
  console.log(`Done. Deleted: ${totalDeleted}, failed: ${totalFailed}.`);
}

cleanup()
  .then(() => process.exit(0))
  .catch(err => {
    console.error('Cleanup failed:', err);
    process.exit(1);
  });
