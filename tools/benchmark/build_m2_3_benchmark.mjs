import fs from "node:fs/promises";
import path from "node:path";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const root = path.resolve(process.cwd());
const docsDir = path.join(root, "docs");
const outputDir = path.join(root, "outputs", "m2_3_professional_benchmark");
const previewDir = path.join("/tmp", "kosch-m2-3-benchmark-preview");

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
      } else if (char === '"') {
        quoted = false;
      } else {
        cell += char;
      }
    } else if (char === '"') {
      quoted = true;
    } else if (char === ",") {
      row.push(cell);
      cell = "";
    } else if (char === "\n") {
      row.push(cell.replace(/\r$/, ""));
      rows.push(row);
      row = [];
      cell = "";
    } else {
      cell += char;
    }
  }
  if (cell.length > 0 || row.length > 0) {
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

function one(value) {
  return Math.round(value * 10) / 10;
}

function clamp(value) {
  return Math.max(0.1, Math.min(10, one(value)));
}

function mean(values) {
  return values.reduce((sum, value) => sum + Number(value), 0) / values.length;
}

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

const baseComparison = parseCsv(
  await fs.readFile(path.join(docsDir, "launcher_comparison_m2_2.csv"), "utf8"),
);
const baseExpertScores = parseCsv(
  await fs.readFile(path.join(docsDir, "expert_scores_m2_2.csv"), "utf8"),
);
const baseExpertOverall = parseCsv(
  await fs.readFile(path.join(docsDir, "expert_launcher_overall_m2_2.csv"), "utf8"),
);

// Only verified M2.3 deltas are applied. Missing measurements stay deliberately low.
const koschUpdates = {
  C01: 8.6, C02: 8.6, C03: 8.4, C04: 8.2, C05: 8.4,
  C06: 7.1, C07: 7.4, C08: 6.7, C09: 8.0, C10: 7.4,
  C11: 8.0, C12: 8.0, C13: 7.4, C15: 7.4, C16: 7.4,
  C18: 7.2, C19: 7.4, C20: 6.7, C21: 5.6, C24: 7.9,
  C25: 9.3, C26: 7.7, C27: 6.8, C30: 8.1, C36: 7.4,
  C37: 8.5, C38: 6.9, C40: 9.0, C41: 9.4, C42: 9.5,
  C45: 6.5, C46: 9.2, C47: 8.0, C48: 6.9, C49: 8.6,
  C50: 4.2, C51: 7.4, C52: 5.2, C53: 6.3, C54: 7.8,
  C55: 9.4, C57: 8.9, C58: 5.3, C59: 7.5, C60: 8.0,
  C65: 8.9,
};

const launcherNames = [
  "KoSch M2.3",
  "Pixel / Android 17",
  "Nova 8 / 8.1 Beta",
  "Niagara 1.x",
  "Smart Launcher 6.6",
  "Microsoft Launcher",
  "Lawnchair 15 Beta 3",
];

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
  ["C66", "Professionelles Command Center", "Product", 8.8, 8.0, 6.8, 7.3, 8.0, 9.0, 6.5],
  ["C67", "Hardware-Tastatur-Produktivität", "UX", 8.6, 8.8, 6.8, 7.2, 7.6, 8.2, 7.2],
  ["C68", "Datensparsame Kontaktaktion", "System", 9.2, 8.8, 1.2, 1.2, 1.5, 7.5, 6.5],
  ["C69", "Portable Backup-Vertraulichkeit und Integrität", "Security", 8.8, 8.7, 7.5, 7.0, 7.8, 8.5, 6.2],
  ["C70", "Restore-Vorschau, Validierung und Kontrolle", "Launcher", 8.7, 7.8, 8.8, 7.8, 8.6, 8.2, 6.8],
  ["C71", "Metadatenarmes lokales Audit", "Engineering", 8.2, 7.0, 5.5, 5.0, 5.5, 7.0, 7.0],
  ["C72", "Capability-Policy und Risikogates", "Security", 8.5, 9.0, 8.0, 8.2, 8.0, 8.0, 8.5],
  ["C73", "Widget-Größenvertrag und Presets", "Launcher", 7.5, 9.2, 9.0, 8.8, 9.0, 8.5, 8.5],
  ["C74", "Shortcut-Discoverability und Recovery", "UX", 8.8, 8.6, 6.8, 7.2, 7.6, 8.2, 7.2],
  ["C75", "Automatisierte Release- und Sicherheitsgates", "Engineering", 7.6, 9.5, 7.8, 7.6, 7.6, 8.2, 8.5],
];
comparisonRows.push(...newCategories.map((row) => [...row, "", 0]));

for (const row of comparisonRows) {
  const scores = row.slice(3, 10).map(Number);
  const best = Math.max(...scores);
  row[10] = launcherNames.filter((_, index) => Math.abs(scores[index] - best) < 0.001).join(" / ");
  row[11] = one(best - scores[0]);
}

const comparisonHeader = [
  "ID", "Kategorie", "Bereich", ...launcherNames, "Führend", "KoSch_Lücke_zur_Spitze",
];
await fs.writeFile(
  path.join(docsDir, "launcher_comparison_m2_3.csv"),
  toCsv([comparisonHeader, ...comparisonRows]),
);

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

const koschById = new Map(comparisonRows.map((row) => [row[0], Number(row[3])]));
const categoryIds = comparisonRows.map((row) => row[0]);
const expertScoreRows = baseExpertScores.slice(1).map((source) => {
  const role = source[0];
  const values = categoryIds.map((id, index) => {
    if (index < 65) {
      const delta = koschById.get(id) - priorKoSch.get(id);
      return clamp(Number(source[index + 1]) + delta);
    }
    return clamp(koschById.get(id) + (roleOffsets[role] ?? 0));
  });
  return [role, ...values, one(mean(values))];
});
const expertScoreHeader = ["ExpertRole", ...categoryIds, "Overall"];
await fs.writeFile(
  path.join(docsDir, "expert_scores_m2_3.csv"),
  toCsv([expertScoreHeader, ...expertScoreRows]),
);

const expertScoreByRole = new Map(expertScoreRows.map((row) => [row[0], row]));
const expertOverallRows = baseExpertOverall.slice(1).map((source) => {
  const role = source[0];
  const roleOffset = roleOffsets[role] ?? 0;
  const koschOverall = expertScoreByRole.get(role).at(-1);
  const competitors = source.slice(2).map((oldOverall, competitorIndex) => {
    const newScores = newCategories.map((row) => clamp(Number(row[4 + competitorIndex]) + roleOffset));
    return one((Number(oldOverall) * 65 + newScores.reduce((sum, value) => sum + value, 0)) / 75);
  });
  return [role, koschOverall, ...competitors];
});
const expertOverallHeader = ["ExpertRole", ...launcherNames];
await fs.writeFile(
  path.join(docsDir, "expert_launcher_overall_m2_3.csv"),
  toCsv([expertOverallHeader, ...expertOverallRows]),
);

const rawGeneralAverages = launcherNames.map((_, index) =>
  mean(comparisonRows.map((row) => row[3 + index])),
);
const rawExpertAverages = launcherNames.map((_, index) =>
  mean(expertOverallRows.map((row) => row[1 + index])),
);
const generalAverages = rawGeneralAverages.map(one);
const expertAverages = rawExpertAverages.map(one);
const areas = [...new Set(comparisonRows.map((row) => row[2]))];
const areaAverages = Object.fromEntries(areas.map((area) => [
  area,
  launcherNames.map((_, index) => one(mean(
    comparisonRows.filter((row) => row[2] === area).map((row) => row[3 + index]),
  ))),
]));
const largestGaps = [...comparisonRows]
  .sort((left, right) => Number(right[11]) - Number(left[11]))
  .slice(0, 15)
  .map((row) => ({ id: row[0], category: row[1], kosch: Number(row[3]), leader: row[10], gap: Number(row[11]) }));
const leadershipCount = comparisonRows.filter((row) => Number(row[11]) === 0).length;

await fs.mkdir(previewDir, { recursive: true });
await fs.writeFile(path.join(previewDir, "metrics.json"), JSON.stringify({
  generalAverages,
  expertAverages,
  areaAverages,
  leadershipCount,
  largestGaps,
  roleScores: expertScoreRows.map((row) => ({ role: row[0], overall: row.at(-1) })),
}, null, 2));

const workbook = Workbook.create();
const summary = workbook.worksheets.add("Summary");
const comparison = workbook.worksheets.add("Comparison");
const expertScores = workbook.worksheets.add("Expert Scores");
const expertOverall = workbook.worksheets.add("Expert Overall");
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
  sheet.getRange(range).format = {
    fill: navy,
    font: { bold: true, color: white, size: 20 },
    verticalAlignment: "center",
  };
  const first = range.split(":")[0];
  const row = Number(first.match(/\d+/)[0]) + 1;
  const startCol = first.match(/[A-Z]+/)[0];
  const endCol = range.split(":")[1].match(/[A-Z]+/)[0];
  const subtitleRange = `${startCol}${row}:${endCol}${row + 1}`;
  sheet.getRange(subtitleRange).merge();
  sheet.getRange(subtitleRange).values = [[subtitle]];
  sheet.getRange(subtitleRange).format = {
    fill: navy,
    font: { color: mist, size: 10 },
    wrapText: true,
    verticalAlignment: "center",
  };
}

function styleHeader(range) {
  range.format = {
    fill: surface,
    font: { bold: true, color: white },
    wrapText: true,
    verticalAlignment: "center",
    borders: { preset: "outside", style: "thin", color: line },
  };
  range.format.rowHeight = 32;
}

function styleBody(range) {
  range.format = {
    fill: pale,
    font: { color: "#15303A", size: 9 },
    verticalAlignment: "center",
    borders: {
      insideHorizontal: { style: "thin", color: "#D7E3E8" },
      bottom: { style: "thin", color: "#D7E3E8" },
    },
  };
}

const comparisonStartRow = 6;
const comparisonEndRow = comparisonStartRow + comparisonRows.length - 1;
const priorGeneral = one(mean([...priorKoSch.values()]));
const ranking = launcherNames
  .map((name, index) => [name, generalAverages[index], expertAverages[index], index, rawGeneralAverages[index]])
  .sort((left, right) => right[4] - left[4]);
const publishedRanks = ranking.map((row) => {
  const firstIndex = ranking.findIndex((candidate) => candidate[1] === row[1]);
  const tieCount = ranking.filter((candidate) => candidate[1] === row[1]).length;
  return tieCount > 1 ? `${firstIndex + 1}=` : String(firstIndex + 1);
});

titleBand(
  summary,
  "A1:M1",
  "KoSch M2.3 · Professional Launcher Benchmark",
  "75 Kategorien · Android-17-Systemreferenz + 5 bekannte Launcher · 25 simulierte Fachperspektiven · Stand 24.08.2026",
);
summary.getRange("A5:D5").values = [["Rang", "Launcher", "Allgemein", "25 Rollen"]];
styleHeader(summary.getRange("A5:D5"));
summary.getRange("A6:B12").values = ranking.map((row, index) => [publishedRanks[index], row[0]]);
summary.getRange("C6:D12").formulas = ranking.map((row) => {
  const comparisonColumn = columnName(3 + row[3]);
  const expertColumn = columnName(1 + row[3]);
  return [
    `=AVERAGE('Comparison'!${comparisonColumn}$${comparisonStartRow}:${comparisonColumn}$${comparisonEndRow})`,
    `=AVERAGE('Expert Overall'!${expertColumn}$6:${expertColumn}$30)`,
  ];
});
styleBody(summary.getRange("A6:D12"));
summary.getRange("C6:D12").format.numberFormat = "0.0";
summary.getRange("A5:A12").format.columnWidth = 8;
summary.getRange("B5:B12").format.columnWidth = 31;
summary.getRange("C5:D12").format.columnWidth = 16;

summary.getRange("A15:D15").values = [["KPI", "M2.2", "M2.3", "Delta / Status"]];
styleHeader(summary.getRange("A15:D15"));
summary.getRange("A16:C20").values = [
  ["Allgemeiner KoSch-Score", priorGeneral, generalAverages[0]],
  ["25-Rollen-Mittel", 6.8, expertAverages[0]],
  ["Bewertungskategorien", 65, comparisonRows.length],
  ["Führungen (allein/geteilt)", 14, leadershipCount],
  [">9,5-Nachweisgate", 0, 0],
];
summary.getRange("D16").formulas = [["=C16-B16"]];
summary.getRange("D17").formulas = [["=C17-B17"]];
summary.getRange("D18").formulas = [["=C18-B18"]];
summary.getRange("D19").formulas = [["=C19-B19"]];
summary.getRange("D20").values = [["NICHT ERFÜLLT"]];
styleBody(summary.getRange("A16:D20"));
summary.getRange("B16:D17").format.numberFormat = "0.0";
summary.getRange("B18:D20").format.numberFormat = "0";
summary.getRange("D20").format = { fill: warning, font: { bold: true, color: "#5B1713" } };
summary.getRange("A15:D20").format.columnWidth = 24;

summary.getRange("A23:D23").values = [["Strenges Urteil", "Befund", "Beleg", "Konsequenz"]];
styleHeader(summary.getRange("A23:D23"));
summary.getRange("A24:D27").values = [
  ["Pro-tauglicher Alpha-Kern", "Pro Desk, Tastatur, Kontakt, Backup und Audit sind real implementiert", "CI #23 + Quellstand", "M2.3 als belastbare Basis"],
  ["Kein 9,5-Produkt", "Performance, Accessibility, OEM und Recovery sind nicht im Gerätelabor belegt", "Quality Gates offen", "Keine künstliche Hochwertung"],
  ["Privacy-Differenzierer", "Kein INTERNET-Recht, keine Kontakt-Vollberechtigung, verschlüsselter Export", "Manifest + Unit-Tests", "Security-Review und Fuzzing folgen"],
  ["Launcher-Parität offen", "Widget-Stacks, freies Raster, Theme-/Icon-Pack- und Gestentiefe fehlen", "75-Kategorien-Matrix", "M2.4 priorisiert P0-Lücken"],
];
styleBody(summary.getRange("A24:D27"));
summary.getRange("A23:D27").format.wrapText = true;
summary.getRange("A23:D27").format.columnWidth = 29;
summary.getRange("A24:D27").format.rowHeight = 45;

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

titleBand(
  comparison,
  "A1:L1",
  "75-Kategorien-Funktionsvergleich",
  "0,1 = praktisch nicht vorhanden · 10,0 = nachweislich erstklassig · Pixel/Android 17 ist bewusst die anspruchsvolle Systemreferenz.",
);
comparison.getRange("A5:L5").values = [comparisonHeader];
comparison.getRange(`A${comparisonStartRow}:L${comparisonEndRow}`).values = comparisonRows;
styleHeader(comparison.getRange("A5:L5"));
styleBody(comparison.getRange(`A${comparisonStartRow}:L${comparisonEndRow}`));
comparison.getRange(`D${comparisonStartRow}:J${comparisonEndRow}`).format.numberFormat = "0.0";
comparison.getRange(`L${comparisonStartRow}:L${comparisonEndRow}`).format.numberFormat = "0.0";
comparison.getRange(`D${comparisonStartRow}:J${comparisonEndRow}`).conditionalFormats.add("colorScale", {
  colors: [warning, "#FFF3C4", teal],
  thresholds: ["min", "50%", "max"],
});
comparison.getRange(`L${comparisonStartRow}:L${comparisonEndRow}`).conditionalFormats.add("dataBar", {
  color: "#EE6C68",
  thresholds: [0, "max"],
  gradient: true,
});
comparison.getRange(`A5:A${comparisonEndRow}`).format.columnWidth = 8;
comparison.getRange(`B5:B${comparisonEndRow}`).format.columnWidth = 36;
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
titleBand(
  expertScores,
  `A1:${expertOverallColumn}1`,
  "25 Fachperspektiven × 75 KoSch-Kategorien",
  "Simulierte, reproduzierbare Rollen – keine befragten Personen. Bestehende Rollenprofile wurden nur um belegte M2.3-Deltas und dokumentierte Rollenstrenge fortgeschrieben.",
);
expertScores.getRange(`A5:${expertOverallColumn}5`).values = [expertScoreHeader];
expertScores.getRange(`A6:${expertOverallColumn}30`).values = expertScoreRows;
for (let row = 6; row <= 30; row += 1) {
  expertScores.getRange(`${expertOverallColumn}${row}`).formulas = [[
    `=AVERAGE(B${row}:${expertLastCategoryColumn}${row})`,
  ]];
}
styleHeader(expertScores.getRange(`A5:${expertOverallColumn}5`));
styleBody(expertScores.getRange(`A6:${expertOverallColumn}30`));
expertScores.getRange(`B6:${expertOverallColumn}30`).format.numberFormat = "0.0";
expertScores.getRange(`B6:${expertLastCategoryColumn}30`).conditionalFormats.add("colorScale", {
  colors: [warning, "#FFF3C4", teal],
  thresholds: ["min", "50%", "max"],
});
expertScores.getRange("A5:A30").format.columnWidth = 31;
expertScores.getRange(`B5:${expertLastCategoryColumn}30`).format.columnWidth = 7;
expertScores.getRange(`${expertOverallColumn}5:${expertOverallColumn}30`).format.columnWidth = 12;
expertScores.freezePanes.freezeRows(5);
expertScores.freezePanes.freezeColumns(1);

titleBand(
  expertOverall,
  "A1:H1",
  "25-Rollen-Gesamtvergleich",
  "KoSch basiert auf 75 Einzelwerten je Rolle. Konkurrenzwerte kombinieren das frühere 65-Kategorien-Mittel mit zehn neuen, rollenjustierten Kategorien.",
);
expertOverall.getRange("A5:H5").values = [expertOverallHeader];
expertOverall.getRange("A6:H30").values = expertOverallRows;
for (let row = 6; row <= 30; row += 1) {
  expertOverall.getRange(`B${row}`).formulas = [[`='Expert Scores'!${expertOverallColumn}${row}`]];
}
expertOverall.getRange("A32").values = [["Mittel"]];
for (let column = 1; column <= 7; column += 1) {
  const name = columnName(column);
  expertOverall.getRange(`${name}32`).formulas = [[`=AVERAGE(${name}6:${name}30)`]];
}
styleHeader(expertOverall.getRange("A5:H5"));
styleBody(expertOverall.getRange("A6:H30"));
styleHeader(expertOverall.getRange("A32:H32"));
expertOverall.getRange("B6:H32").format.numberFormat = "0.0";
expertOverall.getRange("B6:H30").conditionalFormats.add("colorScale", {
  colors: [warning, "#FFF3C4", teal],
  thresholds: ["min", "50%", "max"],
});
expertOverall.getRange("A5:A32").format.columnWidth = 31;
expertOverall.getRange("B5:H32").format.columnWidth = 19;
expertOverall.freezePanes.freezeRows(5);
expertOverall.freezePanes.freezeColumns(1);

const gapPlans = [
  ["P0", "C27", "Kontakte-Integration", "Native API-37-Mehrfeldauswahl, Work-/Personal-Matrix", "API 29/33/36/37 instrumentiert; keine READ_CONTACTS"],
  ["P0", "C50", "Gemessene Laufzeitperformance", "Macrobenchmark + Baseline-Profile-Generator", "Cold P95 ≤ 1.000 ms; Frame P95 ≤ 16,7 ms"],
  ["P0", "C56", "Lokalisierung", "Deutsch/Englisch vollständig, RTL und Plural", "0 hardcodierte User-Strings; Screenshot- und RTL-Test"],
  ["P0", "C39", "Lernen und Personalisierung", "lokaler Embedding-/Preference-Index mit Reset", "Opt-in, erklärbar, exportier-/löschbar, kein Cloudzwang"],
  ["P0", "C58", "Produktionsreife", "Gerätefarm, Crash-/ANR-Gates und signierter Alpha-Track", "HOME-Recovery auf Pixel/Samsung/weiterem OEM bestanden"],
  ["P0", "C21", "Widget Resize/Restore/Undo", "freie Platzierung, Stacks, Restore-Mapping, Undo", "Provider-/Konfigurations-/Prozesstod-Matrix grün"],
  ["P0", "C08", "Semantische Accessibility", "TalkBack, Switch Access, 200 % Schrift", "keine blockierte/abgeschnittene Primäraktion"],
  ["P0", "C52", "OEM-Kompatibilität", "HOME-/Widget-/Profil-/Stift-Lab", "Samsung + Pixel + weiterer OEM, API 29–37"],
  ["P1", "C35", "Integriertes generatives Modell", "isolierter optionaler lokaler Modellprozess", "SAF-Import, Hash/Lizenz, Cancel/Unload, Thermikgate"],
  ["P1", "C23", "Wallpaper- und Theme-System", "capability-loses Theme Program v1", "Preview, signiertes Paket, atomarer Rollback"],
  ["P1", "C17", "Ordner", "manuelle Ordner, Drag-and-drop und Transaktions-Undo", "Touch/Maus/Tastatur + Prozess-Tod getestet"],
  ["P1", "C48", "Testabdeckung", "instrumentierte State-/Recovery-Matrix", "HOME, SAF, Widget, Backup, Profile, Pen grün"],
  ["P1", "C53", "Akku- und Energieeffizienz", "Batterie-/Thermikmessung", "Idle CPU nahe 0 %, dokumentiertes RSS-/Akku-Budget"],
  ["P1", "C61", "Smartpen-Gerätebeweis", "S Pen, USI, Pixel Pen und Bluetooth-Stift", "P95 Ink-Latenz ≤ 25 ms auf unterstützter Hardware"],
  ["P2", "C16", "Workspace-Anpassung", "Icon Packs, Gesten, Dock-Reorder und Seitenparität", "Migration/Undo und Accessibility vollständig"],
];
titleBand(
  nextRun,
  "A1:E1",
  "M2.4 · Evidenzplan zum 9,5-Gate",
  "Der Abstand wird durch Geräte- und Produktnachweise geschlossen – nicht durch angehobene Selbsteinschätzungen.",
);
nextRun.getRange("A5:E5").values = [["Prio", "Kategorie", "Lücke", "Arbeitspaket", "Abnahmekriterium"]];
nextRun.getRange(`A6:E${5 + gapPlans.length}`).values = gapPlans;
styleHeader(nextRun.getRange("A5:E5"));
styleBody(nextRun.getRange(`A6:E${5 + gapPlans.length}`));
nextRun.getRange(`A5:E${5 + gapPlans.length}`).format.wrapText = true;
nextRun.getRange(`A6:E${5 + gapPlans.length}`).format.rowHeight = 42;
nextRun.getRange(`A5:A${5 + gapPlans.length}`).format.columnWidth = 9;
nextRun.getRange(`B5:B${5 + gapPlans.length}`).format.columnWidth = 12;
nextRun.getRange(`C5:C${5 + gapPlans.length}`).format.columnWidth = 30;
nextRun.getRange(`D5:E${5 + gapPlans.length}`).format.columnWidth = 48;
nextRun.freezePanes.freezeRows(5);

const sourceRows = [
  ["Android 17", "Aktuelle Plattformreferenz", "https://developer.android.com/about/versions/17/"],
  ["Contact Picker", "API-37-Auswahl und Legacy-Extra", "https://developer.android.com/about/versions/17/features/contact-picker"],
  ["Intent API", "System-Contact-Picker-Extra", "https://developer.android.com/reference/android/content/Intent"],
  ["Keyboard", "Hardware-Keyboard und Shortcuts", "https://developer.android.com/develop/ui/views/touch-and-input/keyboard-input"],
  ["Cryptography", "AES/GCM-Empfehlung", "https://developer.android.com/privacy-and-security/cryptography"],
  ["Storage Access Framework", "Nutzergewählte Dokumente", "https://developer.android.com/training/data-storage/shared/documents-files"],
  ["App Widgets", "Provider-Info und Größenvertrag", "https://developer.android.com/reference/android/appwidget/AppWidgetProviderInfo"],
  ["Startup Profiles", "DEX-Layout-Optimierung", "https://developer.android.com/topic/performance/startupprofiles/dex-layout-optimizations"],
  ["Baseline Profiles", "Compose-Performance", "https://developer.android.com/develop/ui/compose/performance/baseline-profiles"],
  ["Macrobenchmark", "Messworkflow", "https://developer.android.com/codelabs/android-macrobenchmark-inspect/"],
  ["Android Stylus", "Stylus-Grundlagen", "https://developer.android.com/develop/ui/views/touch-and-input/stylus-input"],
  ["Adaptive Apps", "Adaptive Layouts", "https://developer.android.com/develop/adaptive-apps/guides/adaptive-dos-and-donts"],
  ["LauncherApps", "Profile und Launcher Activities", "https://developer.android.com/reference/kotlin/android/content/pm/LauncherApps.html"],
  ["Private Space", "Launcher-Pflichten", "https://developer.android.com/about/versions/15/behavior-changes-all"],
  ["Nova", "Offizielle Features", "https://novalauncher.com/"],
  ["Nova Beta", "Offizieller Beta-Stand", "https://novalauncher.com/beta/"],
  ["Niagara", "Pro-Funktionen; aktualisiert 25.06.2026", "https://help.niagaralauncher.app/article/40-niagara-pro-features"],
  ["Smart Launcher 6.6", "Dock, Foldables, Gesten und Migration", "https://docs.smartlauncher.net/faq/changelog/6.6"],
  ["Smart Launcher Backup", "Gerätewechsel und Restore-Grenzen", "https://docs.smartlauncher.net/faq/move-smart-launcher-to-a-new-phone"],
  ["Microsoft Launcher", "Feed, Layoutimport und Work Profile", "https://support.microsoft.com/en-us/office/using-microsoft-launcher-on-android"],
  ["Lawnchair", "Lawnchair 16 Entwicklung; 15 Beta 3 empfohlen", "https://github.com/LawnchairLauncher/lawnchair"],
  ["KoSch CI #23", "Tests, Lint, Debug/Release, Permission Gate", "https://github.com/chekento/kosch-ai-android/actions/runs/32701289727"],
];
titleBand(
  sources,
  "A1:C1",
  "Primärquellen und Bewertungsgrenzen",
  "Hersteller-/Projektquellen am 24.08.2026 geprüft. Ohne identischen Sieben-Launcher-Gerätetest werden unbelegte Eigenschaften konservativ bewertet.",
);
sources.getRange("A5:C5").values = [["Quelle", "Verwendung", "URL"]];
sources.getRange(`A6:C${5 + sourceRows.length}`).values = sourceRows;
styleHeader(sources.getRange("A5:C5"));
styleBody(sources.getRange(`A6:C${5 + sourceRows.length}`));
sources.getRange(`A5:C${5 + sourceRows.length}`).format.wrapText = true;
sources.getRange(`A5:A${5 + sourceRows.length}`).format.columnWidth = 24;
sources.getRange(`B5:B${5 + sourceRows.length}`).format.columnWidth = 45;
sources.getRange(`C5:C${5 + sourceRows.length}`).format.columnWidth = 82;
sources.freezePanes.freezeRows(5);

await fs.mkdir(previewDir, { recursive: true });
const previewSpecs = [
  ["Summary", "A1:M28", 1],
  ["Comparison", "A1:L28", 0.75],
  ["Expert Scores", `A1:N18`, 0.75],
  ["Expert Overall", "A1:H32", 0.9],
  ["Next Run", `A1:E${5 + gapPlans.length}`, 0.9],
  ["Sources", `A1:C${5 + sourceRows.length}`, 0.8],
];
for (const [sheetName, range, scale] of previewSpecs) {
  const preview = await workbook.render({ sheetName, range, scale, format: "png" });
  const fileName = `${sheetName.toLowerCase().replaceAll(" ", "-")}.png`;
  await fs.writeFile(path.join(previewDir, fileName), new Uint8Array(await preview.arrayBuffer()));
}

const keyInspect = await workbook.inspect({
  kind: "table,formula,drawing",
  range: "Summary!A1:M28",
  include: "values,formulas",
  maxChars: 9000,
  tableMaxRows: 28,
  tableMaxCols: 13,
  options: { maxResults: 160 },
});
await fs.writeFile(path.join(previewDir, "inspect-summary.json"), JSON.stringify(keyInspect, null, 2));
const formulaErrors = await workbook.inspect({
  kind: "match",
  searchTerm: "#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A",
  options: { useRegex: true, maxResults: 300 },
  summary: "final formula error scan",
});
await fs.writeFile(path.join(previewDir, "formula-errors.json"), JSON.stringify(formulaErrors, null, 2));

await fs.mkdir(outputDir, { recursive: true });
const xlsx = await SpreadsheetFile.exportXlsx(workbook);
const repositoryWorkbook = path.join(docsDir, "launcher_benchmark_m2_3.xlsx");
const deliverableWorkbook = path.join(outputDir, "KoSch_M2_3_Professional_Launcher_Benchmark.xlsx");
await xlsx.save(repositoryWorkbook);
const exported = await fs.readFile(repositoryWorkbook);
await fs.writeFile(deliverableWorkbook, exported);

console.log(JSON.stringify({
  generalAverages,
  expertAverages,
  areaAverages,
  leadershipCount,
  categoryCount: comparisonRows.length,
  expertRoleCount: expertOverallRows.length,
  previewDir,
  repositoryWorkbook,
  deliverableWorkbook,
}, null, 2));
