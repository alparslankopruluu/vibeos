import {initializeApp} from "firebase-admin/app";
import {getFirestore, FieldValue} from "firebase-admin/firestore";
import {getStorage} from "firebase-admin/storage";
import {onCall, HttpsError} from "firebase-functions/v2/https";
import {defineSecret} from "firebase-functions/params";
import {randomUUID} from "node:crypto";

initializeApp();
const key = defineSecret("OPENAI_API_KEY");
const db = getFirestore();

/** Two free generations per UTC day. Upgrade this policy only after server-side
 * RevenueCat entitlement verification is connected to the Firebase UID. */
export const generateTheme = onCall({region: "us-central1", secrets: [key], timeoutSeconds: 120, memory: "1GiB", maxInstances: 10, enforceAppCheck: true}, async request => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in to create a theme.");
  const prompt = request.data?.prompt;
  const imageBase64 = request.data?.imageBase64;
  if (imageBase64 != null && (typeof imageBase64 !== "string" || imageBase64.length > 2_000_000 || !/^[A-Za-z0-9+/=]+$/.test(imageBase64))) throw new HttpsError("invalid-argument", "Invalid reference image.");
  if (typeof prompt !== "string" || prompt.trim().length < 8 || prompt.length > 500) throw new HttpsError("invalid-argument", "Describe your look in 8–500 characters.");
  const day = new Date().toISOString().slice(0, 10);
  const ref = db.doc(`generationLimits/${uid}_${day}`);
  await db.runTransaction(async tx => {
    const snap = await tx.get(ref);
    const count = snap.data()?.count ?? 0;
    if (count >= 2) throw new HttpsError("resource-exhausted", "Today's two free creations are used.");
    tx.set(ref, {count: count + 1, uid, day, updatedAt: FieldValue.serverTimestamp()}, {merge: true});
  });
  try {
    const instruction = `Create a refined, original vertical phone wallpaper. No typography, logos, trademarks, UI, phone mockup or frames. Visually rich, safe to place app icons over. ${imageBase64 ? "Use the attached user's photo as visual inspiration for color and subject, while creating original wallpaper art. " : ""}Theme: ${prompt.trim()}`;
    const form = new FormData();
    if (imageBase64) {
      const bytes = Buffer.from(imageBase64, "base64");
      if (bytes.length < 1024 || bytes.length > 1_500_000 || bytes[0] !== 0xff || bytes[1] !== 0xd8) throw new HttpsError("invalid-argument", "Upload a smaller JPEG image.");
      form.set("image", new Blob([new Uint8Array(bytes)], {type: "image/jpeg"}), "reference.jpg");
    }
    form.set("model", "gpt-image-1"); form.set("size", "1024x1536"); form.set("quality", "medium"); form.set("n", "1"); form.set("prompt", instruction);
    const response = await fetch(imageBase64 ? "https://api.openai.com/v1/images/edits" : "https://api.openai.com/v1/images/generations", {
      method: "POST", headers: {Authorization: `Bearer ${key.value()}`}, body: form, signal: AbortSignal.timeout(90000),
    });
    if (!response.ok) throw new Error(`Image provider responded ${response.status}`);
    const body = await response.json() as {data?: {b64_json?: string}[]};
    const b64 = body.data?.[0]?.b64_json;
    if (!b64) throw new Error("Image provider returned no image");
    const object = getStorage().bucket().file(`generated/${uid}/${randomUUID()}.png`);
    await object.save(Buffer.from(b64, "base64"), {contentType: "image/png", resumable: false, metadata: {cacheControl: "private, max-age=3600"}});
    const [wallpaperUrl] = await object.getSignedUrl({action: "read", expires: Date.now() + 24 * 60 * 60 * 1000});
    return {wallpaperUrl};
  } catch (error) {
    await ref.update({count: FieldValue.increment(-1)}).catch(() => undefined);
    if (error instanceof HttpsError) throw error;
    console.error("generateTheme failed", error);
    throw new HttpsError("internal", "Wallpaper generation failed. Please try again.");
  }
});
