const pptxgen = require("pptxgenjs");
const React = require("react");
const RDS = require("react-dom/server");
const sharp = require("sharp");
const fa = require("react-icons/fa6");
const { applyTheme } = require("/root/.claude/skills/synced/d9625b92-19ee-40ef-96b3-97c8153b7a05_079e8314-d2f1-4710-bce4-1fc05a035446/pptx/scripts/apply_theme.js");
const sizeOf = (f) => require("sharp")(f).metadata();

const D = __dirname + "/img/";
const OUT = process.argv[2] || __dirname + "/faro-presentation-commerciale.pptx";

const THEME = {
  name: "faro",
  headFontFace: "Cambria",
  bodyFontFace: "Calibri",
  colors: {
    dk1: "2B1A14", lt1: "FFFFFF", dk2: "6B4E44", lt2: "FBEEE8",
    accent1: "C63F24", accent2: "E8674A", accent3: "2F7A55", accent4: "C98A1B",
    accent5: "8C6B60", accent6: "F4D6CB", hlink: "A8341B", folHlink: "8C6B60",
  },
};
const HEX = THEME.colors;

async function icon(Comp, color, px = 256) {
  const svg = RDS.renderToStaticMarkup(React.createElement(Comp, { size: px, color: "#" + color }));
  const buf = await sharp(Buffer.from(svg)).png().toBuffer();
  return "image/png;base64," + buf.toString("base64");
}

(async () => {
  const pres = new pptxgen();
  pres.layout = "LAYOUT_16x9"; // 10 x 5.625
  pres.theme = { headFontFace: THEME.headFontFace, bodyFontFace: THEME.bodyFontFace };
  pres.title = "faro – présentation commerciale";
  pres.author = "faro";
  const C = pres.SchemeColor;

  // ---------- Layouts ----------
  pres.defineSlideMaster({
    title: "FARO_TITRE",
    background: { color: HEX.dk1 },
    objects: [
      { placeholder: { options: { name: "title", type: "title", x: 0.6, y: 1.3, w: 4.5, h: 1.9, fontFace: THEME.headFontFace, fontSize: 28, bold: true, color: C.background1, align: "left", valign: "top", margin: 0 }, text: "" } },
      { placeholder: { options: { name: "body", type: "body", x: 0.6, y: 3.45, w: 4.4, h: 1.0, fontSize: 15, color: C.accent6, align: "left", valign: "top", margin: 0 }, text: "" } },
    ],
  });
  pres.defineSlideMaster({
    title: "FARO_CONTENU",
    background: { color: HEX.lt2 },
    margin: [0.4, 0.5, 0.45, 0.5],
    objects: [
      { placeholder: { options: { name: "title", type: "title", x: 0.5, y: 0.25, w: 9.0, h: 0.75, fontFace: THEME.headFontFace, fontSize: 24, bold: true, color: C.text1, align: "left", valign: "middle", margin: 0 }, text: "" } },
      { text: { text: "faro · présentation commerciale", options: { x: 0.5, y: 5.25, w: 4, h: 0.25, fontSize: 9, color: C.text2, margin: 0 } } },
    ],
    slideNumber: { x: 9.0, y: 5.25, w: 0.5, h: 0.25, fontSize: 9, color: HEX.dk2, align: "right", margin: 0 },
  });

  // ---------- helpers ----------
  // image on white card (motif) ; fits inside box keeping ratio
  async function carteImage(slide, file, x, y, w, h, name, align = "center") {
    const m = await sizeOf(D + file);
    const r = m.width / m.height;
    let iw = w, ih = w / r;
    if (ih > h) { ih = h; iw = h * r; }
    const ix = align === "left" ? x : align === "right" ? x + w - iw : x + (w - iw) / 2;
    const iy = y + (h - ih) / 2;
    const pad = 0.06;
    slide.addShape(pres.shapes.ROUNDED_RECTANGLE, {
      x: ix - pad, y: iy - pad, w: iw + 2 * pad, h: ih + 2 * pad, rectRadius: 0.08,
      fill: { color: C.background1 }, line: { color: C.accent6, width: 0.75 },
      shadow: { type: "outer", color: HEX.dk1, opacity: 0.18, blur: 8, offset: 2, angle: 90 },
      objectName: name + " – cadre",
    });
    slide.addImage({ path: D + file, x: ix, y: iy, w: iw, h: ih, altText: name, objectName: name });
    return { x: ix, y: iy, w: iw, h: ih };
  }
  function pastille(slide, n, x, y, d = 0.36) {
    slide.addShape(pres.shapes.OVAL, { x, y, w: d, h: d, fill: { color: C.accent1 }, line: { type: "none" }, objectName: "pastille " + n });
    slide.addText(String(n), { x, y, w: d, h: d, align: "center", valign: "middle", fontSize: 13, bold: true, color: C.background1, margin: 0, isTextBox: true });
  }
  function points(slide, items, x, y, w, h, size = 13) {
    const runs = [];
    items.forEach((it, i) => {
      const [t, d] = Array.isArray(it) ? it : [it, null];
      runs.push({ text: t, options: { bold: !!d, color: C.text1, breakLine: !!d } });
      if (d) runs.push({ text: d, options: { color: C.text2, breakLine: i < items.length - 1, paraSpaceAfter: 9 } });
      else if (i < items.length - 1) runs[runs.length - 1].options.breakLine = true;
    });
    slide.addText(runs, { x, y, w, h, fontSize: size, valign: "top", margin: 0, isTextBox: true, paraSpaceAfter: 9 });
  }
  function encadre(slide, txt, x, y, w, h, name) {
    slide.addShape(pres.shapes.ROUNDED_RECTANGLE, { x, y, w, h, rectRadius: 0.08, fill: { color: C.accent1 }, line: { type: "none" }, objectName: name });
    slide.addText(txt, { x: x + 0.15, y, w: w - 0.3, h, fontSize: 13, color: C.background1, valign: "middle", margin: 0, isTextBox: true, italic: true });
  }

  // ======== 1. Titre ========
  pres.addSection({ title: "Ouverture" });
  let s = pres.addSlide({ masterName: "FARO_TITRE", sectionTitle: "Ouverture" });
  s.addText("faro", { x: 0.6, y: 0.55, w: 3, h: 0.6, fontFace: THEME.headFontFace, fontSize: 30, bold: true, color: C.accent2, margin: 0, isTextBox: true });
  s.addText("De la visite à la marge réelle, un seul outil pour l'entreprise de rénovation", { placeholder: "title" });
  s.addText("Présentation commerciale · novembre 2026", { placeholder: "body" });
  await carteImage(s, "tableau.jpg", 5.4, 0.9, 4.3, 3.9, "Tableau de bord faro");
  s.addNotes("Objectif de la réunion : que tu (le commercial) comprennes en 20 minutes à qui on vend faro, quel problème il règle, et ce que tu peux montrer en démo dès demain.\n\nfaro est né d'un besoin réel : une chargée d'études en rénovation nous a décrit son quotidien, et le produit est construit autour de ce déroulé.");

  // ======== 2. Le problème ========
  pres.addSection({ title: "Le problème" });
  s = pres.addSlide({ masterName: "FARO_CONTENU", sectionTitle: "Le problème" });
  s.addText("Aujourd'hui, une affaire de rénovation se perd entre Excel, papier et logiciel de devis", { placeholder: "title", fontSize: 21 });
  const pb = [
    ["~90 %", "des estimations « à la grosse »", "sont fausses : le client est mal orienté, la marge ou l'affaire se perd."],
    ["1 jour", "pour rédiger un devis", "dans un logiciel de devis classique, avec des prix de bibliothèque sans lien avec les coûts réels."],
    ["2 vues", "pour un seul chantier", "le client pense en pièces, le conducteur et les artisans en lots. Aujourd'hui on choisit l'une ou l'autre."],
    ["0 retour", "du chantier vers le bureau", "avenants, dépenses réelles et marge sont suivis à part, souvent trop tard."],
  ];
  pb.forEach(([big, lab, txt], i) => {
    const x = 0.5 + i * 2.28;
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x, y: 1.3, w: 2.1, h: 3.1, rectRadius: 0.1, fill: { color: C.background1 }, line: { color: C.accent6, width: 0.75 }, objectName: "carte problème " + (i + 1) });
    s.addText(big, { x: x + 0.15, y: 1.45, w: 1.8, h: 0.8, fontFace: THEME.headFontFace, fontSize: 34, bold: true, color: C.accent1, margin: 0, isTextBox: true });
    s.addText(lab, { x: x + 0.15, y: 2.25, w: 1.8, h: 0.55, fontSize: 13, bold: true, color: C.text1, margin: 0, valign: "top", isTextBox: true });
    s.addText(txt, { x: x + 0.15, y: 2.85, w: 1.8, h: 1.45, fontSize: 11.5, color: C.text2, margin: 0, valign: "top", isTextBox: true });
  });
  s.addText("Source : entretien avec une chargée d'études en entreprise de rénovation (octobre 2026).", { x: 0.5, y: 4.6, w: 9, h: 0.3, fontSize: 10, italic: true, color: C.text2, margin: 0, isTextBox: true });
  s.addNotes("Ces chiffres viennent du terrain, pas d'une étude de marché : c'est le témoignage de notre chargée d'études. À présenter comme tel (« ce qu'on entend chez nos interlocuteurs »).\n\nQuestion à poser au prospect : « Combien de temps vous prend un devis ? Votre estimation de départ est-elle souvent juste ? Comment suivez-vous les travaux supplémentaires ? »");

  // ======== 3. La solution : le fil de l'affaire ========
  pres.addSection({ title: "La solution" });
  s = pres.addSlide({ masterName: "FARO_CONTENU", sectionTitle: "La solution" });
  s.addText("faro suit l'affaire d'un bout à l'autre, sans ressaisie", { placeholder: "title" });
  const etapes = [
    [fa.FaCamera, "Visite", "Notes et photos sur le téléphone, compte rendu PDF"],
    [fa.FaRulerCombined, "Estimation", "Fourchette par pièce à partir de ratios €/m²"],
    [fa.FaFileInvoice, "Devis", "Pièces × lots, gammes, PDF client et chantier"],
    [fa.FaHelmetSafety, "Chantier", "Devis figé, bons de commande par lot"],
    [fa.FaFileSignature, "Avenants", "Chiffrés, envoyés et validés par le client"],
    [fa.FaChartLine, "Marge", "Prévue vs réelle, lot par lot"],
  ];
  for (let i = 0; i < etapes.length; i++) {
    const [Ic, t, d] = etapes[i];
    const x = 0.5 + i * 1.53;
    s.addShape(pres.shapes.OVAL, { x: x + 0.38, y: 1.35, w: 0.7, h: 0.7, fill: { color: C.accent1 }, line: { type: "none" }, objectName: "icône " + t + " – fond" });
    s.addImage({ data: await icon(Ic, "FFFFFF"), x: x + 0.56, y: 1.53, w: 0.34, h: 0.34, altText: t, objectName: "icône " + t });
    if (i < etapes.length - 1) s.addShape(pres.shapes.LINE, { x: x + 1.15, y: 1.7, w: 0.65, h: 0, line: { color: C.accent2, width: 1.5, endArrowType: "triangle" }, objectName: "flèche " + (i + 1) });
    s.addText(t, { x, y: 2.2, w: 1.46, h: 0.35, align: "center", fontSize: 14, bold: true, color: C.text1, margin: 0, isTextBox: true });
    s.addText(d, { x, y: 2.55, w: 1.46, h: 0.9, align: "center", fontSize: 11, color: C.text2, margin: 0, valign: "top", isTextBox: true });
  }
  encadre(s, "Chaque information saisie une fois sert à toute l'équipe : le commercial, la chargée d'études, le conducteur de travaux, le gérant… et le client.", 0.5, 3.75, 9, 0.85, "message clé");
  s.addNotes("C'est LA slide à retenir. Les concurrents font très bien une ou deux de ces étapes (le devis, la compta). faro relie toutes les étapes de l'affaire : ce qui est noté en visite se retrouve dans le devis, le devis signé devient le budget du chantier, les avenants et dépenses réelles donnent la marge réelle.");

  // ======== 4. Tableau de bord ========
  s = pres.addSlide({ masterName: "FARO_CONTENU", sectionTitle: "La solution" });
  s.addText("Le gérant voit en un coup d'œil où en est son activité", { placeholder: "title" });
  await carteImage(s, "tableau.jpg", 0.5, 1.15, 5.9, 3.85, "Tableau de bord", "left");
  points(s, [
    ["Montant signé du mois", "et taux de transformation des devis."],
    ["Marge réelle des chantiers", "calculée à partir des dépenses saisies."],
    ["À faire aujourd'hui", "devis à relancer, avenants en attente de réponse client."],
    ["Sur ordinateur et téléphone", "rien à installer : un navigateur suffit."],
  ], 6.75, 1.25, 2.85, 3.7, 12.5);
  s.addNotes("En démo, commencer par cet écran : c'est celui qui parle au gérant, qui est l'acheteur. Montrer les relances (« sans nouvelle depuis 20 jours ») : c'est de l'argent qui dort.");

  // ======== 5. Devis pièces × lots ========
  s = pres.addSlide({ masterName: "FARO_CONTENU", sectionTitle: "La solution" });
  s.addText("Un devis saisi une fois, présenté par pièce ou par lot", { placeholder: "title" });
  await carteImage(s, "devis.jpg", 0.5, 1.1, 5.6, 3.95, "Devis par pièce", "left");
  points(s, [
    ["Par pièce pour le client", "Cuisine, salle de bains, séjour… c'est ainsi qu'il lit un devis."],
    ["Par lot pour le chantier", "plomberie, électricité, carrelage… pour le conducteur et les artisans."],
    ["Bibliothèque d'ouvrages", "prix réutilisables, import et export Excel (CSV)."],
    ["PDF prêts à envoyer", "TVA 5,5 / 10 / 20 % par ligne, totaux calculés automatiquement."],
  ], 6.45, 1.2, 3.1, 3.8, 12.5);
  s.addNotes("C'est la vraie différence métier. Dans les logiciels de devis classiques, on structure le devis par pièces OU par lots. Dans faro, chaque ligne est rattachée aux deux : un clic sur « Par lot » et le même devis devient le dossier du conducteur de travaux.\n\nArgument temps : avec la bibliothèque et l'estimation en amont, l'objectif est de passer d'une journée à quelques heures par devis. À vérifier avec le premier client pilote avant de l'annoncer comme un chiffre.");

  // ======== 6. Gammes + espace client ========
  s = pres.addSlide({ masterName: "FARO_CONTENU", sectionTitle: "La solution" });
  s.addText("Le client compare les gammes et valide en ligne", { placeholder: "title" });
  await carteImage(s, "gammes.jpg", 0.5, 1.15, 4.6, 2.6, "Options Eco / Standard / Premium");
  await carteImage(s, "client.jpg", 5.4, 1.15, 4.1, 2.6, "Espace client");
  s.addText([
    { text: "Eco / Standard / Premium", options: { bold: true, color: C.text1, breakLine: true } },
    { text: "Prix de chaque gamme par pièce et comparatif global : le client choisit, le devis se recalcule.", options: { color: C.text2 } },
  ], { x: 0.5, y: 3.95, w: 4.6, h: 1.1, fontSize: 12, valign: "top", margin: 0, isTextBox: true });
  s.addText([
    { text: "Espace client sur simple lien", options: { bold: true, color: C.text1, breakLine: true } },
    { text: "Le client consulte son devis, télécharge les PDF et accepte ou refuse les avenants, sans créer de compte.", options: { color: C.text2 } },
  ], { x: 5.4, y: 3.95, w: 4.1, h: 1.1, fontSize: 12, valign: "top", margin: 0, isTextBox: true });
  s.addNotes("Argument commercial pour l'entreprise : proposer trois gammes fait monter le panier moyen et accélère la décision. Le client se sent maître de son budget.\n\nL'espace client évite les allers-retours de mails et garde une trace datée de chaque accord.");

  // ======== 7. Avenants + marge ========
  s = pres.addSlide({ masterName: "FARO_CONTENU", sectionTitle: "La solution" });
  s.addText("Les travaux supplémentaires sont facturés, et la marge se suit en continu", { placeholder: "title", fontSize: 22 });
  await carteImage(s, "avenants.jpg", 0.5, 1.15, 4.4, 2.55, "Avenants");
  await carteImage(s, "marge.jpg", 5.1, 1.15, 4.4, 2.55, "Suivi de marge");
  s.addText([
    { text: "Avenants tracés", options: { bold: true, color: C.text1, breakLine: true } },
    { text: "Brouillon → envoyé → validé ou refusé, avec date et nom du signataire. Le marché actuel est toujours à jour.", options: { color: C.text2 } },
  ], { x: 0.5, y: 3.95, w: 4.4, h: 1.1, fontSize: 12, valign: "top", margin: 0, isTextBox: true });
  s.addText([
    { text: "Marge prévue vs réelle", options: { bold: true, color: C.text1, breakLine: true } },
    { text: "Chaque dépense (matériaux, location, sous-traitance) est rattachée à un lot : on voit les dérives pendant le chantier, pas après.", options: { color: C.text2 } },
  ], { x: 5.1, y: 3.95, w: 4.4, h: 1.1, fontSize: 12, valign: "top", margin: 0, isTextBox: true });
  s.addNotes("Les travaux supplémentaires non facturés sont une fuite de marge classique en rénovation : une demande faite oralement sur le chantier, jamais chiffrée. Dans faro, un chantier signé est figé : tout changement passe par un avenant.\n\nQuestion à poser : « Sur un chantier, savez-vous votre marge réelle avant la facture finale ? »");

  // ======== 8. Visite + estimation ========
  s = pres.addSlide({ masterName: "FARO_CONTENU", sectionTitle: "La solution" });
  s.addText("Dès la visite, une estimation réaliste plutôt qu'un chiffre « à la grosse »", { placeholder: "title", fontSize: 22 });
  await carteImage(s, "visite-mobile.jpg", 0.5, 1.1, 2.5, 3.95, "Visite sur téléphone");
  await carteImage(s, "estimation.jpg", 3.35, 1.2, 6.15, 1.5, "Estimation rapide");
  points(s, [
    ["Visite sur le téléphone", "notes et photos classées par pièce, compte rendu PDF en un clic."],
    ["Fourchette d'estimation", "par pièce, selon le type de pièce et le niveau de travaux (ratios €/m² ajustables)."],
    ["Moins de ressaisie", "les pièces et surfaces de la visite servent ensuite au devis."],
  ], 3.4, 3.05, 6.1, 2.0, 12.5);
  s.addNotes("Une fourchette honnête (« entre 48 000 et 84 000 € ») vaut mieux qu'un chiffre unique faux : le client n'est pas déçu au devis détaillé.\n\nLes ratios sont réglés par l'entreprise elle-même. Prochaine étape produit : les alimenter avec les coûts réels des chantiers terminés.");

  // ======== 9. Cible ========
  pres.addSection({ title: "Vendre faro" });
  s = pres.addSlide({ masterName: "FARO_CONTENU", sectionTitle: "Vendre faro" });
  s.addText("La cible : les entreprises de rénovation de 2 à 20 personnes", { placeholder: "title" });
  const pers = [
    [fa.FaUserTie, "Le gérant", "Décide et paie", "Veut savoir ce qu'il gagne sur chaque chantier et ne plus perdre d'affaires sur des devis trop lents."],
    [fa.FaCompassDrafting, "La chargée d'études", "Utilise faro tous les jours", "Veut des devis plus rapides, une bibliothèque de prix fiable et des visites bien documentées."],
    [fa.FaHelmetSafety, "Le conducteur de travaux", "Reçoit le dossier", "Veut un devis par lot, des bons de commande clairs et des avenants validés avant d'agir."],
  ];
  for (let i = 0; i < pers.length; i++) {
    const [Ic, t, r, d] = pers[i];
    const x = 0.5 + i * 3.05;
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x, y: 1.25, w: 2.85, h: 3.15, rectRadius: 0.1, fill: { color: i === 0 ? C.accent1 : C.background1 }, line: { color: C.accent6, width: 0.75 }, objectName: "persona " + t });
    const fg = i === 0 ? "FFFFFF" : HEX.accent1;
    s.addImage({ data: await icon(Ic, fg), x: x + 0.25, y: 1.45, w: 0.5, h: 0.5, altText: t, objectName: "icône " + t });
    s.addText(t, { x: x + 0.25, y: 2.1, w: 2.4, h: 0.4, fontSize: 16, bold: true, fontFace: THEME.headFontFace, color: i === 0 ? C.background1 : C.text1, margin: 0, isTextBox: true });
    s.addText(r, { x: x + 0.25, y: 2.5, w: 2.4, h: 0.3, fontSize: 11.5, bold: true, color: i === 0 ? C.accent6 : C.accent1, margin: 0, isTextBox: true });
    s.addText(d, { x: x + 0.25, y: 2.9, w: 2.4, h: 1.4, fontSize: 12, color: i === 0 ? C.background1 : C.text2, margin: 0, valign: "top", isTextBox: true });
  }
  s.addText("Taille indicative, à confirmer avec les premiers clients.", { x: 0.5, y: 4.6, w: 9, h: 0.3, fontSize: 10, italic: true, color: C.text2, margin: 0, isTextBox: true });
  s.addNotes("On vend au gérant, mais on gagne avec la chargée d'études : c'est elle qui fera la démo à son patron si elle est convaincue. Proposer une démo à deux (gérant + chargée d'études).\n\nÉviter au début les très grosses entreprises (ERP déjà en place) et les artisans seuls (pas de rôle chargé d'études).");

  // ======== 10. Différenciation ========
  s = pres.addSlide({ masterName: "FARO_CONTENU", sectionTitle: "Vendre faro" });
  s.addText("Ce qui distingue faro", { placeholder: "title" });
  const H = (t) => ({ text: t, options: { bold: true, color: C.background1, fill: { color: C.text1 }, align: "center", valign: "middle" } });
  const L = (t) => ({ text: t, options: { color: C.text1, bold: true, valign: "middle" } });
  const V = (t, ok) => ({ text: t, options: { align: "center", valign: "middle", color: ok === true ? C.accent3 : ok === false ? C.text2 : C.accent4, bold: ok === true } });
  const rows = [
    [H(""), H("Excel / papier"), H("Logiciel de devis bâtiment"), { text: "faro", options: { bold: true, color: C.background1, fill: { color: C.accent1 }, align: "center", valign: "middle" } }],
    [L("Visite et photos reliées au devis"), V("Non", false), V("Rarement", null), V("Oui", true)],
    [L("Devis par pièce ET par lot"), V("À la main", false), V("L'un ou l'autre", null), V("Oui, même devis", true)],
    [L("Gammes Eco / Standard / Premium"), V("À la main", false), V("Variable", null), V("Oui", true)],
    [L("Avenants validés par le client en ligne"), V("Non", false), V("Variable", null), V("Oui", true)],
    [L("Marge prévue vs réelle par lot"), V("À la main", false), V("Souvent dans un autre outil", null), V("Oui", true)],
    [L("Prise en main"), V("Immédiate", true), V("Formation", null), V("Navigateur, sans installation", true)],
  ];
  s.addTable(rows, { x: 0.5, y: 1.15, w: 9.0, colW: [3.0, 1.8, 2.2, 2.0], rowH: 0.48, fontSize: 11.5, border: { type: "solid", pt: 0.75, color: HEX.accent6 }, fill: { color: HEX.lt1 }, margin: [0, 0.1, 0, 0.1], objectName: "tableau comparatif" });
  s.addText("Comparaison générale par catégorie d'outil, à affiner logiciel par logiciel (ProGBat, Obat, Tolteck…).", { x: 0.5, y: 4.7, w: 9, h: 0.3, fontSize: 10, italic: true, color: C.text2, margin: 0, isTextBox: true });
  s.addNotes("Ne pas dénigrer les logiciels en place : beaucoup de prospects utilisent ProGBat ou équivalent pour la compta et la facturation. Positionnement : faro est l'outil de l'affaire et du chantier ; il peut cohabiter avec l'outil de compta grâce aux exports Excel.\n\nAvant d'affirmer qu'un concurrent précis ne fait pas quelque chose, vérifier sur sa documentation à jour.");

  // ======== 11. Offres ========
  s = pres.addSlide({ masterName: "FARO_CONTENU", sectionTitle: "Vendre faro" });
  s.addText("Les offres", { placeholder: "title" });
  const offres = [
    ["Essai gratuit", "30 jours", "Jusqu'à 3 utilisateurs, toutes les fonctions, données de démonstration."],
    ["Starter", "Prix à définir", "1 utilisateur : l'indépendant ou la petite structure qui chiffre seule."],
    ["Pro", "Prix à définir", "Jusqu'à 10 utilisateurs : bureau d'études + conducteurs de travaux."],
    ["Business", "Sur devis", "Utilisateurs illimités, accompagnement à la reprise des prix."],
  ];
  offres.forEach(([t, p, d], i) => {
    const x = 0.5 + i * 2.28, mis = i === 2;
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x, y: 1.2, w: 2.1, h: 3.0, rectRadius: 0.1, fill: { color: mis ? C.text1 : C.background1 }, line: { color: mis ? C.text1 : C.accent6, width: 0.75 }, objectName: "offre " + t });
    s.addText(t, { x: x + 0.18, y: 1.4, w: 1.75, h: 0.45, fontSize: 18, bold: true, fontFace: THEME.headFontFace, color: mis ? C.background1 : C.text1, margin: 0, isTextBox: true });
    s.addText(p, { x: x + 0.18, y: 1.9, w: 1.75, h: 0.45, fontSize: 15, bold: true, color: mis ? C.accent2 : C.accent1, margin: 0, isTextBox: true });
    s.addText(d, { x: x + 0.18, y: 2.5, w: 1.75, h: 1.6, fontSize: 12, color: mis ? C.accent6 : C.text2, margin: 0, valign: "top", isTextBox: true });
  });
  encadre(s, "Prix et limites encore à fixer : à décider ensemble après les premiers retours terrain.", 0.5, 4.4, 9, 0.6, "avertissement prix");
  s.addNotes("Les limites d'utilisateurs sont déjà gérées dans l'application ; les prix et le paiement en ligne ne sont pas encore en place.\n\nPour les premiers clients : proposer un pilote (essai prolongé, prix de lancement) en échange de retours réguliers et d'un témoignage.");

  // ======== 12. Objections ========
  s = pres.addSlide({ masterName: "FARO_CONTENU", sectionTitle: "Vendre faro" });
  s.addText("Les objections fréquentes, et quoi répondre", { placeholder: "title" });
  const obj = [
    ["« On a déjà ProGBat (ou autre). »", "faro ne remplace pas la compta. Il gère l'affaire de la visite à la marge, et exporte en Excel. On peut commencer sur une seule affaire."],
    ["« Ressaisir notre bibliothèque de prix, c'est trop long. »", "Import Excel (CSV) de la bibliothèque, réimport pour mettre les prix à jour."],
    ["« Mes données sont-elles en sécurité ? »", "Chaque entreprise a ses données séparées, connexion sécurisée par compte. Hébergement en France : en cours de choix."],
    ["« Mes équipes ne sont pas à l'aise avec l'informatique. »", "Pensé pour le téléphone sur chantier, sans installation ; essai de 30 jours avec données de démo."],
  ];
  obj.forEach(([q, r], i) => {
    const y = 1.15 + i * 0.95;
    pastille(s, i + 1, 0.5, y + 0.05);
    s.addText(q, { x: 1.0, y, w: 3.4, h: 0.8, fontSize: 13, bold: true, color: C.text1, valign: "top", margin: 0, isTextBox: true });
    s.addText(r, { x: 4.55, y, w: 4.95, h: 0.8, fontSize: 12, color: C.text2, valign: "top", margin: 0, isTextBox: true });
  });
  s.addNotes("Hébergement : ne pas promettre de date ni de certification tant que le choix n'est pas fait. Dire « hébergement en France prévu ».\n\nSi l'objection est le prix : rappeler qu'un seul avenant oublié ou une estimation ratée coûte plus cher qu'un an d'abonnement (à chiffrer avec le prospect).");

  // ======== 13. Prochaines étapes ========
  pres.addSection({ title: "Suite" });
  s = pres.addSlide({ masterName: "FARO_TITRE", sectionTitle: "Suite" });
  s.addText("Prochaines étapes", { placeholder: "title" });
  s.addText("faro est en phase pilote : les premiers clients façonnent le produit.", { placeholder: "body" });
  const suite = [
    ["Cette semaine", "Démo en direct sur les données d'exemple (7 affaires réalistes)."],
    ["Sous 1 mois", "Lister 10 entreprises de rénovation à contacter ; viser 2 à 3 pilotes."],
    ["Avec les pilotes", "Fixer les prix, confirmer la coexistence avec ProGBat, recueillir un témoignage."],
  ];
  suite.forEach(([t, d], i) => {
    const y = 1.2 + i * 1.15;
    s.addShape(pres.shapes.OVAL, { x: 5.6, y, w: 0.42, h: 0.42, fill: { color: C.accent1 }, line: { type: "none" }, objectName: "étape " + (i + 1) });
    s.addText(String(i + 1), { x: 5.6, y, w: 0.42, h: 0.42, align: "center", valign: "middle", fontSize: 14, bold: true, color: C.background1, margin: 0, isTextBox: true });
    s.addText(t, { x: 6.2, y: y - 0.02, w: 3.3, h: 0.35, fontSize: 15, bold: true, color: C.accent2, margin: 0, isTextBox: true });
    s.addText(d, { x: 6.2, y: y + 0.33, w: 3.3, h: 0.7, fontSize: 12, color: C.background1, margin: 0, valign: "top", isTextBox: true });
  });
  s.addNotes("Proposer au commercial : 1) qu'il fasse lui-même la démo sur les données d'exemple (compte demo / demo), 2) qu'on construise ensemble la liste des premières entreprises à appeler, 3) qu'on fixe un point dans deux semaines pour décider des prix de lancement.");

  await pres.writeFile({ fileName: OUT });
  await applyTheme(OUT, THEME);
  console.log("ok", OUT);
})().catch((e) => { console.error(e); process.exit(1); });
