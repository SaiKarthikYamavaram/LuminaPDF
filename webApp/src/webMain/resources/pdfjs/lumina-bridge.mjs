// The app's only contact with pdf.js. Kotlin calls these through the luminaPdf global declared in
// pdf-renderer/src/webMain, using plain values both the JS and Wasm targets can read.
import * as pdfjs from './pdf.min.mjs';

const base = new URL('./', import.meta.url).href;
pdfjs.GlobalWorkerOptions.workerSrc = `${base}pdf.worker.min.mjs`;

const getPage = (doc, page) => doc.getPage(page + 1);

// Outline targets are either an explicit destination or the name of one.
async function targetPage(doc, dest) {
  try {
    const explicit = typeof dest === 'string' ? await doc.getDestination(dest) : dest;
    const ref = explicit?.[0];
    if (ref == null) return -1;
    return typeof ref === 'number' ? ref : await doc.getPageIndex(ref);
  } catch {
    return -1;
  }
}

async function mapOutlineItem(doc, item) {
  return {
    title: item.title,
    page: await targetPage(doc, item.dest),
    children: await Promise.all(item.items.map((child) => mapOutlineItem(doc, child))),
  };
}

globalThis.luminaPdf = {
  async open(data, password) {
    try {
      const doc = await pdfjs.getDocument({
        // A copy, because pdf.js transfers this buffer to its worker.
        data: new Uint8Array(data.buffer, data.byteOffset, data.byteLength).slice(),
        password: password ?? undefined,
        cMapUrl: `${base}cmaps/`,
        cMapPacked: true,
        standardFontDataUrl: `${base}standard_fonts/`,
        wasmUrl: `${base}wasm/`,
        isEvalSupported: false,
      }).promise;
      return { status: 'ok', doc };
    } catch (error) {
      if (error?.name !== 'PasswordException') throw error;
      const wrong = error.code === pdfjs.PasswordResponses.INCORRECT_PASSWORD;
      return { status: wrong ? 'wrong-password' : 'password', doc: null };
    }
  },

  pageCount: (doc) => doc.numPages,

  async aspect(doc, page) {
    const viewport = (await getPage(doc, page)).getViewport({ scale: 1 });
    return viewport.height / viewport.width;
  },

  // left and top are pixels in a fullWidth-wide rendering of the page, as in PdfDocument.renderTile.
  async render(doc, page, left, top, width, height, scale, fullWidth) {
    const pdfPage = await getPage(doc, page);
    const pageWidth = pdfPage.getViewport({ scale: 1 }).width;
    const viewport = pdfPage.getViewport({
      scale: (fullWidth / pageWidth) * scale,
      offsetX: -left * scale,
      offsetY: -top * scale,
    });
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const canvasContext = canvas.getContext('2d', { willReadFrequently: true });
    await pdfPage.render({ canvas, canvasContext, viewport }).promise;
    return new Int8Array(canvasContext.getImageData(0, 0, width, height).data.buffer);
  },

  // pdf.js reports text in runs, not characters. Each run's width is shared out by how wide its
  // characters are in the nearest browser font, so edges inside a word are close but not exact.
  async glyphs(doc, page) {
    const pdfPage = await getPage(doc, page);
    const viewport = pdfPage.getViewport({ scale: 1 });
    const content = await pdfPage.getTextContent();
    const measure = document.createElement('canvas').getContext('2d');
    const glyphs = [];
    for (const item of content.items) {
      if (!item.str) continue;
      const transform = pdfjs.Util.transform(viewport.transform, item.transform);
      const fontHeight = Math.hypot(transform[2], transform[3]);
      const characters = Array.from(item.str);
      measure.font = `100px ${content.styles[item.fontName]?.fontFamily ?? 'sans-serif'}`;
      const widths = characters.map((character) => measure.measureText(character).width);
      const unit = item.width / widths.reduce((sum, width) => sum + width, 0);
      let x = transform[4];
      characters.forEach((text, index) => {
        const right = x + widths[index] * unit;
        glyphs.push({
          text,
          left: x / viewport.width,
          top: (transform[5] - fontHeight) / viewport.height,
          right: right / viewport.width,
          bottom: transform[5] / viewport.height,
        });
        x = right;
      });
    }
    return glyphs;
  },

  async outline(doc) {
    const items = (await doc.getOutline()) ?? [];
    return Promise.all(items.map((item) => mapOutlineItem(doc, item)));
  },

  close(doc) {
    doc.loadingTask.destroy();
  },
};
