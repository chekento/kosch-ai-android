import fs from "node:fs/promises";
import path from "node:path";
import { SpreadsheetFile, Workbook } from "@oai/artifact-tool";

const root = path.resolve(process.cwd());
const docsDir = path.join(root, "docs");
const previewDir = path.join("/tmp", "kosch-m2-2-benchmark-preview");

function parseCsv(text) {
  const rows = [];
  let row = [];
  let cell = "";
  let quoted = false;
  for (let i = 0; i < text.length; i += 1) {
    const char = text[i];
    if (quoted) {
      if (char === '"' && text[i + 1] === '"') {
        cell += '"';
        i += 1;
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
  await fs.readFile(path.join(docsDir, "launcher_comparison_m2_1.csv"), "utf8"),
);
const baseExpertScores = parseCsv(
  await fs.readFile(path.join(docsDir, "expert_scores_m2_1.csv"), "utf8"),
);
const baseExpertOverall = parseCsv(
  await fs.readFile(path.join(docsDir, "expert_launcher_overall_m2_1.csv"), "utf8"),
);

const koschUpdates = {
  C01: 8.4, C02: 8.4, C03: 8.2, C04: 7.8, C05: 8.2,
  C06: 6.9, C07: 7.2, C08: 6.2, C09: 7.7, C11: 7.8,
  C12: 7.9, C13: 7.3, C18: 7.0, C23: 5.8, C24: 5.2,
  C25: 9.1, C30: 7.8, C31: 6.0, C36: 6.9, C38: 6.7,
  C41: 9.1, C42: 8.9, C47: 7.7, C48: 5.9, C51: 7.0,
  C52: 5.0, C55: 9.1, C57: 8.7, C58: 4.9, C59: 7.0,
  C60: 7.6,
};

const launcherNames = [
  "KoSch M2.2",
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
  row[4] = Number(row[4]);
  row[5] = Number(row[5]);
  row[6] = Number(row[6]);
  row[7] = Number(row[7]);
  row[8] = Number(row[8]);
  row[9] = Number(row[9]);
  return row;
});

const newCategories = [
  ["C61", "Smartpen-Erkennung und Gerätewechsel", "System", 8.6, 8.8, 2.0, 2.5, 2.5, 2.5, 3.5],
  ["C62", "Druck, Neigung, Hover und Radierer", "System", 8.3, 9.2, 1.5, 1.5, 1.5, 1.5, 2.5],
  ["C63", "Integrierter Pen-Workspace", "Launcher", 8.5, 5.0, 1.5, 1.0, 1.2, 1.5, 2.0],
  ["C64", "Systemhandschrift und IME-Integration", "System", 7.2, 9.0, 6.8, 6.5, 6.5, 6.8, 6.8],
  ["C65", "In-App FAQ und Self-Service", "Product", 8.6, 8.5, 6.5, 9.0, 8.5, 8.2, 7.5],
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
const comparisonCsv = [comparisonHeader, ...comparisonRows];
await fs.writeFile(path.join(docsDir, "launcher_comparison_m2_2.csv"), toCsv(comparisonCsv));

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
    if (index < 60) {
      const delta = koschById.get(id) - priorKoSch.get(id);
      return clamp(Number(source[index + 1]) + delta);
    }
    return clamp(koschById.get(id) + (roleOffsets[role] ?? 0));
  });
  return [role, ...values, one(mean(values))];
});
const expertScoreHeader = ["ExpertRole", ...categoryIds, "Overall"];
const expertScoresCsv = [expertScoreHeader, ...expertScoreRows];
await fs.writeFile(path.join(docsDir, "expert_scores_m2_2.csv"), toCsv(expertScoresCsv));

const expertScoreByRole = new Map(expertScoreRows.map((row) => [row[0], row]));
const newCategoryRows = comparisonRows.slice(-5);
const expertOverallRows = baseExpertOverall.slice(1).map((source) => {
  const role = source[0];
  const roleOffset = roleOffsets[role] ?? 0;
  const koschOverall = expertScoreByRole.get(role).at(-1);
  const competitors = source.slice(2).map((oldOverall, competitorIndex) => {
    const newScores = newCategoryRows.map((row) => clamp(Number(row[4 + competitorIndex]) + roleOffset));
    return one((Number(oldOverall) * 60 + newScores.reduce((sum, value) => sum + value, 0)) / 65);
  });
  return [role, koschOverall, ...competitors];
});
const expertOverallHeader = ["ExpertRole", ...launcherNames];
const expertOverallCsv = [expertOverallHeader, ...expertOverallRows];
await fs.writeFile(path.join(docsDir, "expert_launcher_overall_m2_2.csv"), toCsv(expertOverallCsv));

const generalAverages = launcherNames.map((_, index) => one(mean(comparisonRows.map((row) => row[3 + index]))));
const expertAverages = launcherNames.map((_, index) => one(mean(expertOverallRows.map((row) => row[1 + index]))));
const areas = [...new Set(comparisonRows.map((row) => row[2]))];
const areaAverages = Object.fromEntries(areas.map((area) => [
  area,
  launcherNames.map((_, index) => one(mean(
    comparisonRows.filter((row) => row[2] === area).map((row) => row[3 + index]),
  ))),
]));
const metrics = {
  generalAverages,
  expertAverages,
  areaAverages,
  leadershipCount: comparisonRows.filter((row) => Number(row[11]) === 0).length,
  largestGaps: [...comparisonRows]
    .sort((a, b) => Number(b[11]) - Number(a[11]))
    .slice(0, 12)
    .map((row) => ({ id: row[0], category: row[1], kosch: row[3], leader: row[10], gap: row[11] })),
  roleScores: expertScoreRows.map((row) => ({ role: row[0], overall: row.at(-1) })),
};
await fs.mkdir(previewDir, { recursive: true });
await fs.writeFile(path.join(previewDir, "metrics.json"), JSON.stringify(metrics, null, 2));

const workbook = Workbook.create();
const summary = workbook.worksheets.add("Summary");
const comparison = workbook.worksheets.add("Comparison");
const expertScores = workbook.worksheets.add("Expert Scores");
const expertOverall = workbook.worksheets.add("Expert Overall");
const sources = workbook.worksheets.add("Sources");

const navy = "#07141D";
const surface = "#102733";
const teal = "#69E6D7";
const sky = "#80BFFF";
const violet = "#B7A5FF";
const white = "#F4FBFF";
const mist = "#B7C8D0";
const line = "#31505D";
const warning = "#FFB4A9";

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
  const subRange = `${startCol}${row}:${endCol}${row + 1}`;
  sheet.getRange(subRange).merge();
  sheet.getRange(subRange).values = [[subtitle]];
  sheet.getRange(subRange).format = {
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
    borders: { preset: "all", style: "thin", color: line },
  };
  range.format.rowHeight = 30;
}

function styleBody(range) {
  range.format = {
    font: { color: "#15303A", size: 9 },
    verticalAlignment: "center",
    borders: { preset: "all", style: "thin", color: "#D7E3E8" },
  };
}

titleBand(
  summary,
  "A1:M1",
  "KoSch M2.2 · Launcher Benchmark",
  "65 Kategorien · Android-17-Systemreferenz + 5 bekannte Launcher · 25 simulierte, reproduzierbare Fachperspektiven · Stand 24.08.2026",
);
summary.getRange("A5:D5").values = [["Rang", "Launcher", "Allgemein", "25 Rollen"]];
styleHeader(summary.getRange("A5:D5"));
const ranking = launcherNames
  .map((name, index) => [name, generalAverages[index], expertAverages[index]])
  .sort((a, b) => b[1] - a[1]);
summary.getRange("A6:D12").values = ranking.map((row, index) => [index + 1, ...row]);
styleBody(summary.getRange("A6:D12"));
summary.getRange("C6:D12").format.numberFormat = "0.0";
summary.getRange("A6:A12").format.numberFormat = "0";
summary.getRange("A5:D12").format.columnWidth = 18;
summary.getRange("B5:B12").format.columnWidth = 30;
summary.getRange("A5:A12").format.columnWidth = 8;
summary.getRange("A15:D15").values = [["KPI", "M2.1", "M2.2", "Delta"]];
styleHeader(summary.getRange("A15:D15"));
summary.getRange("A16:B18").values = [
  ["Allgemeiner KoSch-Score", one(mean([...priorKoSch.values()]))],
  ["Bewertungskategorien", 60],
  ["Fachrollen", 25],
];
summary.getRange("C16:C18").values = [[generalAverages[0]], [65], [25]];
summary.getRange("D16").formulas = [["=C16-B16"]];
summary.getRange("D17").formulas = [["=C17-B17"]];
summary.getRange("D18").formulas = [["=C18-B18"]];
styleBody(summary.getRange("A16:D18"));
summary.getRange("B16:D16").format.numberFormat = "0.0";
summary.getRange("B17:D18").format.numberFormat = "0";
summary.getRange("A21:D21").values = [["Strenges Urteil", "Befund", "Nachweis", "Nächster Run"]];
styleHeader(summary.getRange("A21:D21"));
summary.getRange("A22:D24").values = [
  ["Eigenständig", "API-freier Local Core, Pen Space und sichere System-Gateways", "CI #18 grün", "Lokales LLM isolieren"],
  ["Noch nicht führend", "Widget-Tiefe, Backup, Performance und Produktionsreife bleiben zurück", "Scores 0,1–10,0", "Messbare P0-Gates"],
  ["Modernisiert", "Adaptive Shell, Material You, Work-Profile-Badges und Smartpen", "M2.2-Quellstand", "Geräte- und Accessibility-Lab"],
];
styleBody(summary.getRange("A22:D24"));
summary.getRange("A21:D24").format.wrapText = true;
summary.getRange("A21:D24").format.columnWidth = 28;
summary.getRange("A22:D24").format.rowHeight = 42;

summary.getRange("F5:G5").values = [["Launcher", "Score"]];
summary.getRange("F6:G12").formulas = ranking.map((row) => {
  const originalIndex = launcherNames.indexOf(row[0]);
  const comparisonCol = columnName(3 + originalIndex);
  return [[`=B${6 + ranking.findIndex((entry) => entry[0] === row[0])}`, `=AVERAGE(Comparison!${comparisonCol}$6:${comparisonCol}$70)`]];
}).flat();
const chart = summary.charts.add("bar", summary.getRange("F5:G12"));
chart.title = "Gesamtvergleich (0,1–10,0)";
chart.titleTextStyle.fontSize = 13;
chart.hasLegend = false;
chart.xAxis = { axisType: "textAxis", textStyle: { fontSize: 9 } };
chart.yAxis = { numberFormatCode: "0.0", min: 0, max: 10 };
chart.setPosition("F5", "M20");
summary.getRange("F5:G12").format.font = { color: "#617985", size: 8 };
summary.getRange("F5:G12").format.numberFormat = "0.0";
summary.freezePanes.freezeRows(4);

titleBand(
  comparison,
  "A1:L1",
  "65-Kategorien-Funktionsvergleich",
  "0,1 = praktisch nicht vorhanden · 10,0 = nachweislich erstklassig · Pixel/Android 17 ist eine Systemreferenz, kein reiner APK-Vergleich.",
);
comparison.getRange("A5:L5").values = [comparisonHeader];
comparison.getRange("A6:L70").values = comparisonRows;
styleHeader(comparison.getRange("A5:L5"));
styleBody(comparison.getRange("A6:L70"));
comparison.getRange("D6:J70").format.numberFormat = "0.0";
comparison.getRange("L6:L70").format.numberFormat = "0.0";
comparison.getRange("D6:J70").conditionalFormats.add("colorScale", {
  colors: [warning, "#FFF3C4", teal],
  thresholds: ["min", "50%", "max"],
});
comparison.getRange("L6:L70").conditionalFormats.add("dataBar", {
  color: "#EE6C68",
  thresholds: [0, "max"],
  gradient: true,
});
comparison.getRange("A5:A70").format.columnWidth = 8;
comparison.getRange("B5:B70").format.columnWidth = 34;
comparison.getRange("C5:C70").format.columnWidth = 14;
comparison.getRange("D5:J70").format.columnWidth = 17;
comparison.getRange("K5:K70").format.columnWidth = 31;
comparison.getRange("L5:L70").format.columnWidth = 17;
comparison.getRange("B6:B70").format.wrapText = true;
comparison.getRange("A5:L5").format.rowHeight = 42;
comparison.getRange("L5:L70").format.columnWidth = 22;
comparison.freezePanes.freezeRows(5);
comparison.freezePanes.freezeColumns(3);

const expertLastCategoryCol = columnName(65);
const expertOverallCol = columnName(66);
titleBand(
  expertScores,
  `A1:${expertOverallCol}1`,
  "25 Fachperspektiven × 65 KoSch-Kategorien",
  "Simulierte Perspektiven, keine befragten Personen. M2.1-Rollenprofile werden um den nachgewiesenen Kategorienfortschritt und dokumentierte Rollenstrenge fortgeschrieben.",
);
expertScores.getRange(`A5:${expertOverallCol}5`).values = [expertScoreHeader];
expertScores.getRange(`A6:${expertOverallCol}30`).values = expertScoreRows;
for (let row = 6; row <= 30; row += 1) {
  expertScores.getRange(`${expertOverallCol}${row}`).formulas = [[`=AVERAGE(B${row}:${expertLastCategoryCol}${row})`]];
}
styleHeader(expertScores.getRange(`A5:${expertOverallCol}5`));
styleBody(expertScores.getRange(`A6:${expertOverallCol}30`));
expertScores.getRange(`B6:${expertOverallCol}30`).format.numberFormat = "0.0";
expertScores.getRange(`B6:${expertLastCategoryCol}30`).conditionalFormats.add("colorScale", {
  colors: [warning, "#FFF3C4", teal],
  thresholds: ["min", "50%", "max"],
});
expertScores.getRange("A5:A30").format.columnWidth = 30;
expertScores.getRange(`B5:${expertLastCategoryCol}30`).format.columnWidth = 7;
expertScores.getRange(`${expertOverallCol}5:${expertOverallCol}30`).format.columnWidth = 12;
expertScores.freezePanes.freezeRows(5);
expertScores.freezePanes.freezeColumns(1);

titleBand(
  expertOverall,
  "A1:H1",
  "25-Rollen-Gesamtvergleich",
  "KoSch basiert auf 65 Einzelwerten je Rolle. Konkurrenzwerte kombinieren die M2.1-Rollenmittel mit den fünf neuen, rollenjustierten Kategorien.",
);
expertOverall.getRange("A5:H5").values = [expertOverallHeader];
expertOverall.getRange("A6:H30").values = expertOverallRows;
for (let row = 6; row <= 30; row += 1) {
  expertOverall.getRange(`B${row}`).formulas = [[`='Expert Scores'!${expertOverallCol}${row}`]];
}
expertOverall.getRange("A32").values = [["Mittel"]];
for (let col = 1; col <= 7; col += 1) {
  const name = columnName(col);
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

const sourceRows = [
  ["Android 17", "Aktuelle Plattformreferenz, adaptive-first", "https://developer.android.com/about/versions/17/"],
  ["Android Stylus", "Stylus-Grundlagen", "https://developer.android.com/develop/ui/views/touch-and-input/stylus-input"],
  ["Compose Stylus", "Compose-Eingabe", "https://developer.android.com/develop/ui/compose/touch-input/stylus-input"],
  ["Ink API", "Jetpack Ink 1.0.0 Setup", "https://developer.android.com/develop/ui/compose/touch-input/stylus-input/ink-api-setup"],
  ["Handwriting", "Systemhandschrift in Textfeldern", "https://developer.android.com/develop/ui/views/touch-and-input/stylus-input/stylus-input-in-text-fields"],
  ["InputDevice", "Android Eingabegeräte-API", "https://developer.android.com/reference/android/view/InputDevice"],
  ["InputManager", "Live Gerätewechsel", "https://developer.android.com/reference/android/hardware/input/InputManager.InputDeviceListener"],
  ["Adaptive", "Adaptive Dos and Don'ts", "https://developer.android.com/develop/adaptive-apps/guides/adaptive-dos-and-donts"],
  ["Large-screen input", "Stift/Maus/Tastatur auf großen Displays", "https://developer.android.com/develop/ui/views/touch-and-input/input-compatibility-on-large-screens"],
  ["Material 3", "Dynamic Color", "https://developer.android.com/develop/ui/compose/designsystems/material3"],
  ["Material 3 Adaptive", "Adaptive 1.2 stable", "https://developer.android.com/blog/posts/material-3-adaptive-1-2-0-is-stable"],
  ["Insets", "Edge-to-edge und Insets", "https://developer.android.com/develop/ui/compose/system/insets"],
  ["LauncherApps", "Profile und Launcher Activities", "https://developer.android.com/reference/kotlin/android/content/pm/LauncherApps.html"],
  ["Private Space", "Launcher-Pflichten", "https://developer.android.com/about/versions/15/behavior-changes-all"],
  ["Nova", "Offizielle Feature- und Beta-Infos", "https://novalauncher.com/"],
  ["Niagara", "Offizielle Pro-Funktionen", "https://help.niagaralauncher.app/article/40-niagara-pro-features"],
  ["Smart Launcher", "Offizieller Versionsvergleich", "https://docs.smartlauncher.net/faq/start-here/differences-between-versions"],
  ["Microsoft Launcher", "Offizielle Android-Hilfe", "https://support.microsoft.com/en-us/office/using-microsoft-launcher-on-android"],
  ["Lawnchair", "Offizielles Open-Source-Repository", "https://github.com/LawnchairLauncher/lawnchair"],
];
titleBand(
  sources,
  "A1:C1",
  "Primärquellen und Bewertungsgrenzen",
  "Hersteller-/Projektquellen wurden am 24.08.2026 geprüft. Ohne identischen Gerätetest werden unbelegte Eigenschaften konservativ bewertet.",
);
sources.getRange("A5:C5").values = [["Quelle", "Verwendung", "URL"]];
sources.getRange(`A6:C${5 + sourceRows.length}`).values = sourceRows;
styleHeader(sources.getRange("A5:C5"));
styleBody(sources.getRange(`A6:C${5 + sourceRows.length}`));
sources.getRange(`A5:C${5 + sourceRows.length}`).format.wrapText = true;
sources.getRange(`A5:A${5 + sourceRows.length}`).format.columnWidth = 23;
sources.getRange(`B5:B${5 + sourceRows.length}`).format.columnWidth = 39;
sources.getRange(`C5:C${5 + sourceRows.length}`).format.columnWidth = 76;
sources.freezePanes.freezeRows(5);

await fs.mkdir(previewDir, { recursive: true });
const summaryPreview = await workbook.render({
  sheetName: "Summary",
  autoCrop: "all",
  scale: 1,
  format: "png",
});
await fs.writeFile(
  path.join(previewDir, "summary.png"),
  new Uint8Array(await summaryPreview.arrayBuffer()),
);
const comparisonPreview = await workbook.render({
  sheetName: "Comparison",
  range: "A1:L25",
  scale: 0.8,
  format: "png",
});
await fs.writeFile(
  path.join(previewDir, "comparison.png"),
  new Uint8Array(await comparisonPreview.arrayBuffer()),
);

const inspect = await workbook.inspect({
  kind: "workbook,sheet,formula,drawing",
  maxChars: 9000,
  tableMaxRows: 4,
  tableMaxCols: 10,
  options: { maxResults: 120 },
});
await fs.writeFile(path.join(previewDir, "inspect.json"), JSON.stringify(inspect, null, 2));

const xlsx = await SpreadsheetFile.exportXlsx(workbook);
await xlsx.save(path.join(docsDir, "launcher_benchmark_m2_2.xlsx"));

console.log(JSON.stringify({
  generalAverages,
  expertAverages,
  categoryCount: comparisonRows.length,
  expertRoleCount: expertOverallRows.length,
  previewDir,
  workbook: path.join(docsDir, "launcher_benchmark_m2_2.xlsx"),
}, null, 2));
