const MAX_DIMENSION = 900;
// Matches the backend's MAX_PHOTO_DATA_URL_LENGTH, which keeps the item doc
// well under Firestore's 1 MiB limit.
const MAX_DATA_URL_LENGTH = 700_000;
const QUALITY_STEPS = [0.72, 0.6, 0.45];

export interface CompressedPhoto {
  /** "data:image/jpeg;base64,..." - stored inline on the item, like product-lookup photos. */
  dataUrl: string;
  /** Same JPEG as a Blob, for uploading to the backend for identification. */
  blob: Blob;
}

/**
 * Resizes a photo (longest side 900px) and re-encodes it as JPEG in the
 * browser, so full-size phone photos stay under the backend's upload limit
 * and the stored item photo stays small. Mirrors the backend ImageCompressor.
 */
export async function compressPhoto(file: File): Promise<CompressedPhoto> {
  const bitmap = await createImageBitmap(file);
  const scale = Math.min(1, MAX_DIMENSION / Math.max(bitmap.width, bitmap.height));
  const canvas = document.createElement("canvas");
  canvas.width = Math.round(bitmap.width * scale);
  canvas.height = Math.round(bitmap.height * scale);
  const ctx = canvas.getContext("2d");
  if (!ctx) {
    throw new Error("Canvas not supported");
  }
  // JPEG has no alpha - paint white behind transparent PNGs instead of black.
  ctx.fillStyle = "#fff";
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close();

  for (const quality of QUALITY_STEPS) {
    const dataUrl = canvas.toDataURL("image/jpeg", quality);
    if (dataUrl.length <= MAX_DATA_URL_LENGTH) {
      return { dataUrl, blob: await (await fetch(dataUrl)).blob() };
    }
  }
  throw new Error("Photo is too large even after compression");
}
