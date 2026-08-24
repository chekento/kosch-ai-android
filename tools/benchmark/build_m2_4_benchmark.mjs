import fs from "node:fs/promises";
import path from "node:path";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const root = path.resolve(process.cwd());
const docsDir = path.join(root, "docs");
const outputDir = path.join(root, "outputs", "m2_4_professional_benchmark");
const previewDir = path.join("/tmp", "kosch-m2-4-benchmark-preview");

function parseCsv(text) {
  const rows = [];
  let row = [];
  let cell = "";
  let quoted = false;
  for (let index = 0; index < text.length; index += 1) {
    const char = text[index];
    if (quoted) {
      if (char === '"' && text[index + 1] === '"') {
        cell += '"';
        index += 1;
      } else if (char === '"') quoted = false;
      else cell += char;
    } else if (char === '"') quoted = true;
    else if (char === ",") {
      row.push(cell);
      cell = "";
    } else if (char === "\n") {
      row.push(cell.replace(/\r$/, ""));
      rows.push(row);
      row = [];
      cell = "";
    } else cell += char;
  }
  if (cell.length || row.length) {
    row.push(cell.replace(/\r$/, ""));
    rows.push(row);
  }
  return rows;
}

function csvCell(value) {
  if (typeof value === "number") return value.toFixed(1);
  const text = String(value ?? "");
  return /[",\n]/.test(text) ? `"${text.replaceAll('"', '""')}"` : text;
}

function toCsv(rows) {
  return `${rows.map((row) => row.map(csvCell).join(",")).join("\n")}\n`;
}

const one = (value) => Math.round(value * 10) / 10;
const clamp = (value) => Math.max(0.1, Math.min(10, one(value)));
const mean = (values) => values.reduce((sum, value) => sum + Number(value), 0) / values.length;

function columnName(index) {
  let value = index + 1;
  let name = "";
  while (value > 0) {
    const remainder = (value - 1) % 26;
    name = String.fromCharCode(65 + remainder) + name;
    value = Math.floor((value - 1) / 26);
  }
  return name;
}

const baseComparison = parseCsv(await fs.readFile(path.join(docsDir, "launcher_comparison_m2_3.csv"), "utf8"));
const baseExpertScores = parseCsv(await fs.readFile(path.join(docsDir, "expert_scores_m2_3.csv"), "utf8"));
const baseExpertOverall = parseCsv(await fs.readFile(path.join(docsDir, "expert_launcher_overall_m2_3.csv"), "utf8"));

const launcherNames = [
  "KoSch M2.4",
  "Pixel / Android 17",
  "Nova 8 / 8.1 Beta",
  "Niagara 1.x",
  "Smart Launcher 6.6",
  "Microsoft Launcher",
  "Lawnchair 15 Beta 3",
];

// Only repository-verified M2.4 deltas are applied. Missing device measurements remain low.
const koschUpdates = {
  C01: 8.8, C03: 8.5, C04: 8.4, C05: 8.6, C06: 7.2,
  C07: 7.6, C08: 7.3, C09: 8.1, C11: 8.5, C12: 8.3,
  C13: 8.0, C14: 8.1, C15: 8.2, C16: 7.6, C17: 6.7,
  C18: 7.6, C20: 7.2, C21: 6.4, C23: 6.4, C24: 8.5,
  C25: 9.4, C28: 9.3, C29: 7.8, C30: 8.8, C32: 9.4,
  C33: 9.6, C36: 8.2, C37: 8.8, C38: 7.0, C39: 7.6,
  C40: 9.1, C41: 9.5, C42: 9.6, C45: 6.7, C46: 9.4,
  C47: 8.5, C48: 7.6, C49: 8.8, C50: 4.4, C51: 8.4,
  C52: 5.4, C53: 6.4, C54: 8.4, C55: 9.5, C57: 9.2,
  C58: 5.6, C59: 7.8, C60: 8.4, C62: 8.5, C63: 9.0,
  C65: 9.3, C66: 9.1, C67: 8.9, C69: 9.2, C70: 8.8,
  C71: 8.5, C72: 9.0, C73: 8.0, C74: 9.0, C75: 8.4,
};

const priorKoSch = new Map();
const comparisonRows = baseComparison.slice(1).map((source) => {
  const row = [...source];
  const id = row[0];
  priorKoSch.set(id, Number(row[3]));
  row[3] = koschUpdates[id] ?? Number(row[3]);
  for (let index = 4; index <= 9; index += 1) row[index] = Number(row[index]);
  return row;
});

const newCategories = [
  ["C76", "Konfigurationswechsel- und ViewModel-Resilienz", "Engineering", 8.8, 9.6, 8.7, 8.8, 8.8, 8.5, 7.8],
  ["C77", "Prozesssichere Exportübergabe", "Security", 9.0, 9.3, 8.0, 7.8, 8.2, 8.5, 7.5],
  ["C78", "Profilstabile App-Key-Migration", "Launcher", 8.8, 9.5, 9.0, 8.7, 9.2, 9.0, 8.2],
  ["C79", "Adaptives lokales Nutzungsranking", "AI", 8.6, 8.6, 8.8, 9.0, 9.3, 9.2, 7.8],
  ["C80", "Transparenz und Löschung lokaler Lernsignale", "Security", 9.2, 8.8, 7.5, 8.0, 7.8, 7.5, 7.5],
  ["C81", "Verborgene Apps und steuerbare App-Sortierung", "Launcher", 8.5, 9.2, 9.4, 9.0, 9.4, 8.8, 8.7],
  ["C82", "Sichere App-Verwaltung und Deinstallationsgrenze", "System", 8.8, 9.2, 8.7, 8.5, 8.7, 8.8, 8.5],
  ["C83", "Begrenzter SAF-Datei-Arbeitsraum", "System", 9.1, 8.5, 2.5, 2.0, 2.5, 7.0, 2.0],
  ["C84", "Dateioperationen mit Vorschau, Bestätigung und Undo", "Security", 8.8, 9.0, 3.0, 2.5, 3.0, 7.5, 2.5],
  ["C85", "Lokale Dateimetadaten-Intelligenz", "AI", 7.8, 5.0, 1.5, 1.0, 1.5, 4.0, 1.0],
  ["C86", "Systemeinstellungs-Abdeckung und Fallback", "System", 9.0, 9.7, 6.0, 5.0, 5.5, 6.5, 5.5],
  ["C87", "Widget-Reihenfolge und Undo", "Launcher", 8.2, 9.3, 9.2, 9.0, 9.2, 8.7, 8.8],
  ["C88", "Pen-SVG und Integrität sehr langer Striche", "System", 9.0, 8.5, 1.0, 1.0, 1.0, 1.0, 2.0],
  ["C89", "Explizite Semantik und Accessibility-Aktionen", "UX", 7.8, 9.3, 8.0, 8.5, 8.2, 8.3, 7.8],
  ["C90", "Evidenztreue und Release-Nachvollziehbarkeit", "Product", 9.1, 9.5, 8.3, 8.0, 8.2, 8.8, 8.8],
];
comparisonRows.push(...newCategories.map((row) => [...row, "", 0]));

for (const row of comparisonRows) {
  const scores = row.slice(3, 10).map(Number);
  const best = Math.max(...scores);
  row[10] = launcherNames.filter((_, index) => Math.abs(scores[index] - best) < 0.001).join(" / ");
  row[11] = one(best - scores[0]);
}

const comparisonHeader = ["ID", "Kategorie", "Bereich", ...launcherNames, "Führend", "KoSch_Lücke_zur_Spitze"];
await fs.writeFile(path.join(docsDir, "launcher_comparison_m2_4.csv"), toCsv([comparisonHeader, ...comparisonRows]));

const roleOffsets = {
  "Android Launcher Architect": -0.1,
  "Android Framework Engineer": -0.1,
  "Jetpack Compose Engineer": 0.0,
  "Mobile UX Director": 0.1,
  "Visual Design Lead": 0.0,
  "Interaction Designer": 0.1,
  "Accessibility Auditor": -0.3,
  "Privacy Engineer": 0.2,
  "Mobile Security Engineer": 0.1,
  "Applied AI Architect": -0.1,
  "On-device ML Engineer": -0.1,
  "LLM Safety Researcher": 0.1,
  "Open-source Compliance Counsel": 0.2,
  "Product Manager": 0.0,
  "Android Power User": 0.1,
  "Accessibility User Advocate": -0.2,
  "SAF and File Systems Expert": 0.0,
  "Telephony Integration Engineer": 0.0,
  "Widget and Shortcut Expert": -0.1,
  "Mobile Performance Engineer": -0.2,
  "Battery and Thermal Engineer": -0.2,
  "QA Automation Lead": -0.2,
  "Reliability/SRE Engineer": -0.2,
  "Google Play Policy Reviewer": 0.1,
  "Competitive Product Analyst": 0.0,
};

const roleFocus = {
  "Android Launcher Architect": ["Launcher", "System"],
  "Android Framework Engineer": ["Engineering", "System"],
  "Jetpack Compose Engineer": ["UX", "Engineering"],
  "Mobile UX Director": ["UX", "Product"],
  "Visual Design Lead": ["UX"],
  "Interaction Designer": ["UX", "Launcher"],
  "Accessibility Auditor": ["UX"],
  "Privacy Engineer": ["Security"],
  "Mobile Security Engineer": ["Security", "Engineering"],
  "Applied AI Architect": ["AI"],
  "On-device ML Engineer": ["AI", "Engineering"],
  "LLM Safety Researcher": ["AI", "Security"],
  "Open-source Compliance Counsel": ["Product", "Security"],
  "Product Manager": ["Product", "UX"],
  "Android Power User": ["Launcher", "UX"],
  "Accessibility User Advocate": ["UX"],
  "SAF and File Systems Expert": ["System", "Security"],
  "Telephony Integration Engineer": ["System"],
  "Widget and Shortcut Expert": ["Launcher"],
  "Mobile Performance Engineer": ["Engineering"],
  "Battery and Thermal Engineer": ["Engineering"],
  "QA Automation Lead": ["Engineering"],
  "Reliability/SRE Engineer": ["Engineering"],
  "Google Play Policy Reviewer": ["Product", "Security"],
  "Competitive Product Analyst": ["Product", "Launcher"],
};

const koschById = new Map(comparisonRows.map((row) => [row[0], Number(row[3])]));
const areaById = new Map(comparisonRows.map((row) => [row[0], row[2]]));
const categoryIds = comparisonRows.map((row) => row[0]);
const expertScoreRows = baseExpertScores.slice(1).map((source) => {
  const role = source[0];
  const values = categoryIds.map((id, index) => {
    if (index < 75) {
      const delta = koschById.get(id) - priorKoSch.get(id);
      return clamp(Number(source[index + 1]) + delta);
    }
    const focusPenalty = roleFocus[role]?.includes(areaById.get(id)) ? -0.2 : 0;
    return clamp(koschById.get(id) + (roleOffsets[role] ?? 0) + focusPenalty);
  });
  return [role, ...values, one(mean(values))];
});
const expertScoreHeader = ["ExpertRole", ...categoryIds, "Overall"];
await fs.writeFile(path.join(docsDir, "expert_scores_m2_4.csv"), toCsv([expertScoreHeader, ...expertScoreRows]));

const expertScoreByRole = new Map(expertScoreRows.map((row) => [row[0], row]));
const expertOverallRows = baseExpertOverall.slice(1).map((source) => {
  const role = source[0];
  const roleOffset = roleOffsets[role] ?? 0;
  const koschOverall = expertScoreByRole.get(role).at(-1);
  const competitors = source.slice(2).map((oldOverall, competitorIndex) => {
    const added = newCategories.map((row) => {
      const focusPenalty = roleFocus[role]?.includes(row[2]) ? -0.2 : 0;
      return clamp(Number(row[4 + competitorIndex]) + roleOffset + focusPenalty);
    });
    return one((Number(oldOverall) * 75 + added.reduce((sum, value) => sum + value, 0)) / comparisonRows.length);
  });
  return [role, koschOverall, ...competitors];
});
const expertOverallHeader = ["ExpertRole", ...launcherNames];
await fs.writeFile(path.join(docsDir, "expert_launcher_overall_m2_4.csv"), toCsv([expertOverallHeader, ...expertOverallRows]));

const rawGeneralAverages = launcherNames.map((_, index) => mean(comparisonRows.map((row) => row[3 + index])));
const rawExpertAverages = launcherNames.map((_, index) => mean(expertOverallRows.map((row) => row[1 + index])));
const generalAverages = rawGeneralAverages.map(one);
const expertAverages = rawExpertAverages.map(one);
const areas = [...new Set(comparisonRows.map((row) => row[2]))];
const areaAverages = Object.fromEntries(areas.map((area) => [
  area,
  launcherNames.map((_, index) => one(mean(comparisonRows.filter((row) => row[2] === area).map((row) => row[3 + index])))),
]));
const leadershipCount = comparisonRows.filter((row) => Number(row[11]) === 0).length;
const largestGaps = [...comparisonRows]
  .sort((left, right) => Number(right[11]) - Number(left[11]))
  .slice(0, 15)
  .map((row) => ({ id: row[0], category: row[1], kosch: Number(row[3]), leader: row[10], gap: Number(row[11]) }));

await fs.mkdir(previewDir, { recursive: true });
await fs.writeFile(path.join(previewDir, "metrics.json"), JSON.stringify({
  generalAverages, expertAverages, areaAverages, leadershipCount, largestGaps,
  roleScores: expertScoreRows.map((row) => ({ role: row[0], overall: row.at(-1) })),
}, null, 2));

const workbook = Workbook.create();
const summary = workbook.worksheets.add("Summary");
const comparison = workbook.worksheets.add("Comparison");
const expertScores = workbook.worksheets.add("Expert Scores");
const expertOverall = workbook.worksheets.add("Expert Overall");
const featureMap = workbook.worksheets.add("Feature Map");
const nextRun = workbook.worksheets.add("Next Run");
const sources = workbook.worksheets.add("Sources");

const navy = "#07141D";
const surface = "#102733";
const teal = "#69E6D7";
const sky = "#80BFFF";
const white = "#F4FBFF";
const mist = "#B7C8D0";
const line = "#31505D";
const warning = "#FFB4A9";
const pale = "#F5FAFC";

function titleBand(sheet, range, title, subtitle) {
  sheet.showGridLines = false;
  sheet.getRange(range).merge();
  sheet.getRange(range).values = [[title]];
  sheet.getRange(range).format = { fill: navy, font: { bold: true, color: white, size: 20 }, verticalAlignment: "center" };
  const first = range.split(":")[0];
  const row = Number(first.match(/\d+/)[0]) + 1;
  const startCol = first.match(/[A-Z]+/)[0];
  const endCol = range.split(":")[1].match(/[A-Z]+/)[0];
  const subtitleRange = `${startCol}${row}:${endCol}${row + 1}`;
  sheet.getRange(subtitleRange).merge();
  sheet.getRange(subtitleRange).values = [[subtitle]];
  sheet.getRange(subtitleRange).format = { fill: navy, font: { color: mist, size: 10 }, wrapText: true, verticalAlignment: "center" };
}

function styleHeader(range) {
  range.format = { fill: surface, font: { bold: true, color: white }, wrapText: true, verticalAlignment: "center", borders: { preset: "outside", style: "thin", color: line } };
  range.format.rowHeight = 32;
}

function styleBody(range) {
  range.format = { fill: pale, font: { color: "#15303A", size: 9 }, verticalAlignment: "center", borders: { insideHorizontal: { style: "thin", color: "#D7E3E8" }, bottom: { style: "thin", color: "#D7E3E8" } } };
}

const comparisonStartRow = 6;
const comparisonEndRow = comparisonStartRow + comparisonRows.length - 1;
const priorGeneral = one(mean([...priorKoSch.values()]));
const priorExpert = one(mean(baseExpertOverall.slice(1).map((row) => Number(row[1]))));
const ranking = launcherNames
  .map((name, index) => [name, generalAverages[index], expertAverages[index], index, rawGeneralAverages[index]])
  .sort((left, right) => right[4] - left[4]);
const publishedRanks = ranking.map((row) => {
  const firstIndex = ranking.findIndex((candidate) => candidate[1] === row[1]);
  const tieCount = ranking.filter((candidate) => candidate[1] === row[1]).length;
  return tieCount > 1 ? `${firstIndex + 1}=` : String(firstIndex + 1);
});

titleBand(summary, "A1:M1", "KoSch M2.4 · Professional Launcher Benchmark", "90 Kategorien · Android-17-Systemreferenz + 5 bekannte Launcher · 25 simulierte Fachperspektiven · Stand 24.08.2026");
summary.getRange("A5:D5").values = [["Rang", "Launcher", "Allgemein", "25 Rollen"]];
styleHeader(summary.getRange("A5:D5"));
summary.getRange("A6:B12").values = ranking.map((row, index) => [publishedRanks[index], row[0]]);
summary.getRange("C6:D12").formulas = ranking.map((row) => {
  const comparisonColumn = columnName(3 + row[3]);
  const expertColumn = columnName(1 + row[3]);
  return [`=AVERAGE('Comparison'!${comparisonColumn}$${comparisonStartRow}:${comparisonColumn}$${comparisonEndRow})`, `=AVERAGE('Expert Overall'!${expertColumn}$6:${expertColumn}$30)`];
});
styleBody(summary.getRange("A6:D12"));
summary.getRange("C6:D12").format.numberFormat = "0.0";
summary.getRange("A5:A12").format.columnWidth = 8;
summary.getRange("B5:B12").format.columnWidth = 31;
summary.getRange("C5:D12").format.columnWidth = 16;

summary.getRange("A15:D15").values = [["KPI", "M2.3", "M2.4", "Delta / Status"]];
styleHeader(summary.getRange("A15:D15"));
summary.getRange("A16:C20").values = [
  ["Allgemeiner KoSch-Score", priorGeneral, generalAverages[0]],
  ["25-Rollen-Mittel", priorExpert, expertAverages[0]],
  ["Bewertungskategorien", 75, comparisonRows.length],
  ["Führungen (allein/geteilt)", 21, leadershipCount],
  [">9,5-Nachweisgate", 0, 0],
];
for (let row = 16; row <= 19; row += 1) summary.getRange(`D${row}`).formulas = [[`=C${row}-B${row}`]];
summary.getRange("D20").values = [["NICHT ERFÜLLT"]];
styleBody(summary.getRange("A16:D20"));
summary.getRange("B16:D17").format.numberFormat = "0.0";
summary.getRange("B18:D20").format.numberFormat = "0";
summary.getRange("D20").format = { fill: warning, font: { bold: true, color: "#5B1713" } };
summary.getRange("A15:D20").format.columnWidth = 24;

summary.getRange("A23:D23").values = [["Strenges Urteil", "Befund", "Beleg", "Konsequenz"]];
styleHeader(summary.getRange("A23:D23"));
summary.getRange("A24:D28").values = [
  ["Breiter professioneller Alpha-Kern", "Scoped Files, lokale Personalisierung, Systemwege, Pen und Widgets sind integriert", "Quellstand + CI", "M2.4 ist praktisch testbar"],
  ["Resilienz deutlich verbessert", "ViewModel, Einmal-Exporttokens und App-Key-Migration schließen konkrete Datenverlustpfade", "Unit-Tests + Code", "Gerätetest folgt"],
  ["Kein 9,5-Produkt", "Performance, TalkBack, OEM, Android 17 und Release-Signing sind nicht im Lab belegt", "Quality Gates offen", "Keine künstliche Hochwertung"],
  ["Privacy-Differenzierer", "Kein INTERNET-Recht; SAF-Baum statt Vollspeicher; Lernsignale löschbar", "Manifest + Policy", "Security-Review und Provider-Fuzzing folgen"],
  ["Launcher-Parität offen", "Widget-Stacks, freie Raster, Icon-Packs, Gestentiefe und Private Space fehlen", "90-Kategorien-Matrix", "Nächster Run schließt P0-Lücken"],
];
styleBody(summary.getRange("A24:D28"));
summary.getRange("A23:D28").format.wrapText = true;
summary.getRange("A23:D28").format.columnWidth = 29;
summary.getRange("A24:D28").format.rowHeight = 43;

summary.getRange("F5:G5").values = [["Launcher", "Score"]];
summary.getRange("F6:F12").formulas = ranking.map((_, index) => [`=B${index + 6}`]);
summary.getRange("G6:G12").formulas = ranking.map((row) => {
  const comparisonColumn = columnName(3 + row[3]);
  return [`=AVERAGE('Comparison'!${comparisonColumn}$${comparisonStartRow}:${comparisonColumn}$${comparisonEndRow})`];
});
const chart = summary.charts.add("bar", summary.getRange("F5:G12"));
chart.title = "Gesamtvergleich (0,1–10,0)";
chart.titleTextStyle.fontSize = 13;
chart.hasLegend = false;
chart.xAxis = { axisType: "textAxis", textStyle: { fontSize: 9 } };
chart.yAxis = { numberFormatCode: "0.0", min: 0, max: 10 };
chart.setPosition("F5", "M21");
summary.getRange("F5:G12").format.font = { color: "#617985", size: 8 };
summary.getRange("G6:G12").format.numberFormat = "0.0";
summary.freezePanes.freezeRows(4);

titleBand(comparison, "A1:L1", "90-Kategorien-Funktionsvergleich", "0,1 = praktisch nicht vorhanden · 10,0 = nachweislich erstklassig · unbelegte Eigenschaften bleiben konservativ.");
comparison.getRange("A5:L5").values = [comparisonHeader];
comparison.getRange(`A${comparisonStartRow}:L${comparisonEndRow}`).values = comparisonRows;
styleHeader(comparison.getRange("A5:L5"));
styleBody(comparison.getRange(`A${comparisonStartRow}:L${comparisonEndRow}`));
comparison.getRange(`D${comparisonStartRow}:J${comparisonEndRow}`).format.numberFormat = "0.0";
comparison.getRange(`L${comparisonStartRow}:L${comparisonEndRow}`).format.numberFormat = "0.0";
comparison.getRange(`D${comparisonStartRow}:J${comparisonEndRow}`).conditionalFormats.add("colorScale", { colors: [warning, "#FFF3C4", teal], thresholds: ["min", "50%", "max"] });
comparison.getRange(`L${comparisonStartRow}:L${comparisonEndRow}`).conditionalFormats.add("dataBar", { color: "#EE6C68", thresholds: [0, "max"], gradient: true });
comparison.getRange(`A5:A${comparisonEndRow}`).format.columnWidth = 8;
comparison.getRange(`B5:B${comparisonEndRow}`).format.columnWidth = 38;
comparison.getRange(`C5:C${comparisonEndRow}`).format.columnWidth = 14;
comparison.getRange(`D5:J${comparisonEndRow}`).format.columnWidth = 18;
comparison.getRange(`K5:K${comparisonEndRow}`).format.columnWidth = 32;
comparison.getRange(`L5:L${comparisonEndRow}`).format.columnWidth = 22;
comparison.getRange(`B${comparisonStartRow}:B${comparisonEndRow}`).format.wrapText = true;
comparison.getRange("A5:L5").format.rowHeight = 45;
comparison.freezePanes.freezeRows(5);
comparison.freezePanes.freezeColumns(3);

const expertLastCategoryColumn = columnName(categoryIds.length);
const expertOverallColumn = columnName(categoryIds.length + 1);
titleBand(expertScores, `A1:${expertOverallColumn}1`, "25 Fachperspektiven × 90 KoSch-Kategorien", "Simulierte, reproduzierbare Rollen – keine befragten Personen. Rollenfokus verschärft jeweils relevante Bereiche um 0,2 Punkte.");
expertScores.getRange(`A5:${expertOverallColumn}5`).values = [expertScoreHeader];
expertScores.getRange(`A6:${expertOverallColumn}30`).values = expertScoreRows;
for (let row = 6; row <= 30; row += 1) expertScores.getRange(`${expertOverallColumn}${row}`).formulas = [[`=AVERAGE(B${row}:${expertLastCategoryColumn}${row})`]];
styleHeader(expertScores.getRange(`A5:${expertOverallColumn}5`));
styleBody(expertScores.getRange(`A6:${expertOverallColumn}30`));
expertScores.getRange(`B6:${expertOverallColumn}30`).format.numberFormat = "0.0";
expertScores.getRange(`B6:${expertLastCategoryColumn}30`).conditionalFormats.add("colorScale", { colors: [warning, "#FFF3C4", teal], thresholds: ["min", "50%", "max"] });
expertScores.getRange("A5:A30").format.columnWidth = 31;
expertScores.getRange(`B5:${expertLastCategoryColumn}30`).format.columnWidth = 7;
expertScores.getRange(`${expertOverallColumn}5:${expertOverallColumn}30`).format.columnWidth = 12;
expertScores.freezePanes.freezeRows(5);
expertScores.freezePanes.freezeColumns(1);

titleBand(expertOverall, "A1:H1", "25-Rollen-Gesamtvergleich", "KoSch basiert auf 90 Einzelwerten je Rolle; Konkurrenzwerte kombinieren das frühere 75-Kategorien-Mittel mit 15 neuen, rollenjustierten Kategorien.");
expertOverall.getRange("A5:H5").values = [expertOverallHeader];
expertOverall.getRange("A6:H30").values = expertOverallRows;
for (let row = 6; row <= 30; row += 1) expertOverall.getRange(`B${row}`).formulas = [[`='Expert Scores'!${expertOverallColumn}${row}`]];
expertOverall.getRange("A32").values = [["Mittel"]];
for (let column = 1; column <= 7; column += 1) {
  const name = columnName(column);
  expertOverall.getRange(`${name}32`).formulas = [[`=AVERAGE(${name}6:${name}30)`]];
}
styleHeader(expertOverall.getRange("A5:H5"));
styleBody(expertOverall.getRange("A6:H30"));
styleHeader(expertOverall.getRange("A32:H32"));
expertOverall.getRange("B6:H32").format.numberFormat = "0.0";
expertOverall.getRange("B6:H30").conditionalFormats.add("colorScale", { colors: [warning, "#FFF3C4", teal], thresholds: ["min", "50%", "max"] });
expertOverall.getRange("A5:A32").format.columnWidth = 31;
expertOverall.getRange("B5:H32").format.columnWidth = 19;
expertOverall.freezePanes.freezeRows(5);
expertOverall.freezePanes.freezeColumns(1);

const featureRows = [
  ["Offline HOME", "Implementiert", "LauncherApps, Local Core, kein INTERNET-Recht", "Keine generative On-device-Inferenz"],
  ["Konfigurationsresilienz", "Automatisiert", "ViewModel-owned Controller", "Prozesstod instrumentiert offen"],
  ["Export-Recovery", "Automatisiert", "Private Einmaldatei + Saved-State-Token", "OEM-DocumentsProvider-Lab offen"],
  ["App-Identität", "Automatisiert", "Legacy-Key-Migration auf User-Seriennummer", "Work-/Private-Gerätematrix offen"],
  ["Lokale Personalisierung", "Implementiert", "Count + letzter Start, 512 Signale, Reset", "Kein semantischer Präferenzindex"],
  ["App-Raum", "Implementiert", "Hidden, Smart/A–Z/Häufig/Zuletzt", "Keine freie Drawer-Tab-Konfiguration"],
  ["App-Verwaltung", "Implementiert", "Info, Store, Hide, Pin, Ordner, System-Uninstall", "Keine Batch-Operationen"],
  ["Datei-Arbeitsraum", "Implementiert", "Scoped SAF Tree, Navigation, Suche, Create/Rename/Delete", "Kein Copy/Move/Batch; Provider-Lab offen"],
  ["Systemwege", "Implementiert", "14 dokumentierte Settings-/HOME-Routen mit Fallback", "Keine simulierten Systemschalter"],
  ["Widgets", "Implementiert", "Host, Größenpresets, Reihenfolge, Undo", "Keine Stacks/freie Platzierung/Restore"],
  ["Smartpen", "Automatisiert", "Historical points, Endpunkt-Resampling, SVG, A11y-Aktionen", "Latenz-/Gerätelabor offen"],
  ["Accessibility", "Teilbelegt", "Kontrasttests, Merge-Semantik, Custom Actions, Reduced Motion", "TalkBack/Switch/200-%-Lab offen"],
];
titleBand(featureMap, "A1:D1", "M2.4 Feature- und Evidenzkarte", "Status trennt Implementierung, automatisierten Nachweis und weiterhin offene Geräteabnahme.");
featureMap.getRange("A5:D5").values = [["Fähigkeit", "Status", "Nachweis", "Ehrliche Grenze"]];
featureMap.getRange(`A6:D${5 + featureRows.length}`).values = featureRows;
styleHeader(featureMap.getRange("A5:D5"));
styleBody(featureMap.getRange(`A6:D${5 + featureRows.length}`));
featureMap.getRange(`A5:D${5 + featureRows.length}`).format.wrapText = true;
featureMap.getRange(`A6:D${5 + featureRows.length}`).format.rowHeight = 42;
featureMap.getRange(`A5:A${5 + featureRows.length}`).format.columnWidth = 28;
featureMap.getRange(`B5:B${5 + featureRows.length}`).format.columnWidth = 18;
featureMap.getRange(`C5:D${5 + featureRows.length}`).format.columnWidth = 48;
featureMap.freezePanes.freezeRows(5);

const gapPlans = [
  ["P0", "C50", "Gemessene Performance", "Macrobenchmark + Baseline-Profile-Generator", "Cold P95 ≤ 1.000 ms; Frame P95 ≤ 16,7 ms"],
  ["P0", "C08/C89", "Accessibility-Gerätelab", "TalkBack, Switch Access, 200 % Schrift, Bold Text", "Keine blockierte, doppelte oder abgeschnittene Primäraktion"],
  ["P0", "C51/C76/C77", "Prozesstod und HOME-Recovery", "Instrumentierte Kill/Rotate/Fold/Export-Matrix", "Pixel, Samsung und weiterer OEM vollständig grün"],
  ["P0", "C52", "OEM-/Profil-Kompatibilität", "API 29/33/36/37, Personal/Work/paused/locked", "Keine App-Key-, Badge- oder HOME-Kollision"],
  ["P0", "C21/C87", "Widget-Parität", "Freies Raster, Resize-Griffe, Stacks, Restore-Mapping", "Provider-, Konfigurations- und Gerätewechselmatrix grün"],
  ["P0", "C83/C84", "SAF-Provider-Härtung", "Downloads, ExternalStorage, Drive und OEM-Provider", "500er-Grenze, Revoke, Rename/Delete/Fehlerfälle grün"],
  ["P0", "C58/C59/C75", "Release Engineering", "Signierung, SBOM, Dependency Scan, Alpha Track", "Reproduzierbare signierte APK/AAB und Rollback"],
  ["P0", "C56", "Lokalisierung", "Deutsch/Englisch, RTL, Plural und String-Ressourcen", "0 hardcodierte UI-Strings; Screenshot-Tests"],
  ["P1", "C35", "Optionales lokales LLM", "Isolierter llama.cpp/LiteRT-LM-Prozess", "SAF-Modellimport, Hash/Lizenz, Cancel/Unload, Thermikgate"],
  ["P1", "C23", "Theme-/Icon-Pack-System", "Deklarative Theme Programs, Icon Packs, Preview/Rollback", "LCARS bleibt optionales Paket"],
  ["P1", "C16/C17", "Launcher-Anpassung", "Manuelle Ordner, Dock-Reorder, Seiten und Gesten", "Touch/Maus/Tastatur + Undo + Migration"],
  ["P1", "C61-C64/C88", "Smartpen-Gerätebeweis", "S Pen, USI, Pixel Pen, Bluetooth-Stift", "P95 Ink-Latenz ≤ 25 ms"],
  ["P1", "C27", "Android-17-Kontakte", "ACTION_PICK_CONTACTS Mehrfeldauswahl", "API-37-Test ohne READ_CONTACTS"],
  ["P1", "C43-C45", "Aktiver Provider-Sicherheitsmodul", "Separater Network Flavor, Redaction, Auth, Injection-Evals", "Default bleibt offline; Schlüssel nie im Launcher-Prozesslog"],
  ["P2", "C80", "Persönlicher lokaler Index", "Erklärbare Preferences/Embeddings pro Quelle", "Opt-in, Export, Reset und Löschung je Quelle"],
];
titleBand(nextRun, "A1:E1", "Nächster Run · Evidenzplan zum 9,5-Gate", "Die Lücke wird durch Messungen und Produktparität geschlossen – nicht durch angehobene Selbsteinschätzungen.");
nextRun.getRange("A5:E5").values = [["Prio", "Kategorie", "Lücke", "Arbeitspaket", "Abnahmekriterium"]];
nextRun.getRange(`A6:E${5 + gapPlans.length}`).values = gapPlans;
styleHeader(nextRun.getRange("A5:E5"));
styleBody(nextRun.getRange(`A6:E${5 + gapPlans.length}`));
nextRun.getRange(`A5:E${5 + gapPlans.length}`).format.wrapText = true;
nextRun.getRange(`A6:E${5 + gapPlans.length}`).format.rowHeight = 42;
nextRun.getRange(`A5:A${5 + gapPlans.length}`).format.columnWidth = 9;
nextRun.getRange(`B5:B${5 + gapPlans.length}`).format.columnWidth = 14;
nextRun.getRange(`C5:C${5 + gapPlans.length}`).format.columnWidth = 31;
nextRun.getRange(`D5:E${5 + gapPlans.length}`).format.columnWidth = 49;
nextRun.freezePanes.freezeRows(5);

const sourceRows = [
  ["Android 17", "Aktuelle Systemreferenz", "https://developer.android.com/about/versions/17/"],
  ["Android 17 Release Notes", "Plattformstabilität und aktuelle Änderungen", "https://developer.android.com/about/versions/17/release-notes"],
  ["Android 17 SDK", "API 37 und Kompatibilitätsziel", "https://developer.android.com/about/versions/17/setup-sdk"],
  ["Storage Access Framework", "Gewählter Dokumentbaum und Persist Grants", "https://developer.android.com/training/data-storage/shared/documents-files"],
  ["DocumentsContract", "Provider-Flags und Dateioperationen", "https://developer.android.com/reference/android/provider/DocumentsContract"],
  ["ViewModel", "Konfigurationswechsel und UI-State-Ownership", "https://developer.android.com/topic/libraries/architecture/viewmodel"],
  ["Compose Semantics", "Accessibility-Semantik", "https://developer.android.com/develop/ui/compose/accessibility/semantics"],
  ["Android Stylus", "Historical Events und erweiterte Stiftfunktionen", "https://developer.android.com/develop/ui/views/touch-and-input/stylus-input/advanced-stylus-features"],
  ["LauncherApps", "Profile und Launcher Activities", "https://developer.android.com/reference/kotlin/android/content/pm/LauncherApps.html"],
  ["App Widgets", "Host und Größenvertrag", "https://developer.android.com/develop/ui/views/appwidgets/host"],
  ["Macrobenchmark", "Messworkflow", "https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview"],
  ["Nova", "UI, Icons, Backup, Gesten und Drawer-Ordner", "https://novalauncher.com/"],
  ["Nova Beta", "8.1.1 Beta und Zuverlässigkeitshinweis", "https://novalauncher.com/beta/"],
  ["Niagara Pro", "Themes, Anycons, Stacks und Pop-ups", "https://help.niagaralauncher.app/article/40-niagara-pro-features"],
  ["Smart Launcher 6.6", "Dock, Gesten, Foldables und Migration", "https://docs.smartlauncher.net/faq/changelog/6.6"],
  ["Smart Launcher Backup", "Restore-Grenzen", "https://docs.smartlauncher.net/faq/move-smart-launcher-to-a-new-phone"],
  ["Microsoft Launcher", "Feed, Layoutimport und Work Profile", "https://support.microsoft.com/en-us/office/using-microsoft-launcher-on-android"],
  ["Lawnchair", "Lawnchair 16 Entwicklung; 15 Beta 3 empfohlen", "https://github.com/LawnchairLauncher/lawnchair"],
  ["KoSch CI #30", "Tests, Lint, Debug/Release, Permission und Checksum", "https://github.com/chekento/kosch-ai-android/actions/runs/32709807342"],
];
titleBand(sources, "A1:C1", "Primärquellen und Bewertungsgrenzen", "Offizielle Hersteller-/Projektquellen am 24.08.2026 geprüft. Ohne identischen Gerätetest bleiben Eigenschaften konservativ.");
sources.getRange("A5:C5").values = [["Quelle", "Verwendung", "URL"]];
sources.getRange(`A6:C${5 + sourceRows.length}`).values = sourceRows;
styleHeader(sources.getRange("A5:C5"));
styleBody(sources.getRange(`A6:C${5 + sourceRows.length}`));
sources.getRange(`A5:C${5 + sourceRows.length}`).format.wrapText = true;
sources.getRange(`A5:A${5 + sourceRows.length}`).format.columnWidth = 25;
sources.getRange(`B5:B${5 + sourceRows.length}`).format.columnWidth = 48;
sources.getRange(`C5:C${5 + sourceRows.length}`).format.columnWidth = 84;
sources.freezePanes.freezeRows(5);

const previewSpecs = [
  ["Summary", "A1:M29", 1],
  ["Comparison", "A1:L30", 0.75],
  ["Expert Scores", "A1:N18", 0.75],
  ["Expert Overall", "A1:H32", 0.9],
  ["Feature Map", `A1:D${5 + featureRows.length}`, 0.9],
  ["Next Run", `A1:E${5 + gapPlans.length}`, 0.9],
  ["Sources", `A1:C${5 + sourceRows.length}`, 0.8],
];
for (const [sheetName, range, scale] of previewSpecs) {
  const preview = await workbook.render({ sheetName, range, scale, format: "png" });
  await fs.writeFile(path.join(previewDir, `${sheetName.toLowerCase().replaceAll(" ", "-")}.png`), new Uint8Array(await preview.arrayBuffer()));
}

const keyInspect = await workbook.inspect({ kind: "table,formula,drawing", range: "Summary!A1:M29", include: "values,formulas", maxChars: 9000, tableMaxRows: 29, tableMaxCols: 13, options: { maxResults: 180 } });
await fs.writeFile(path.join(previewDir, "inspect-summary.json"), keyInspect.ndjson ?? JSON.stringify(keyInspect, null, 2));
const formulaErrors = await workbook.inspect({ kind: "match", searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A", options: { useRegex: true, maxResults: 300 }, summary: "final formula error scan" });
await fs.writeFile(path.join(previewDir, "formula-errors.json"), formulaErrors.ndjson ?? JSON.stringify(formulaErrors, null, 2));

await fs.mkdir(outputDir, { recursive: true });
const xlsx = await SpreadsheetFile.exportXlsx(workbook);
const repositoryWorkbook = path.join(docsDir, "launcher_benchmark_m2_4.xlsx");
const deliverableWorkbook = path.join(outputDir, "KoSch_M2_4_Professional_Launcher_Benchmark.xlsx");
await xlsx.save(repositoryWorkbook);
await fs.copyFile(repositoryWorkbook, deliverableWorkbook);

console.log(JSON.stringify({
  generalAverages, expertAverages, areaAverages, leadershipCount,
  categoryCount: comparisonRows.length, expertRoleCount: expertOverallRows.length,
  previewDir, repositoryWorkbook, deliverableWorkbook,
}, null, 2));
