const STATUS_LABELS = {
  normal: "Normal",
  warning: "Sınıra Yaklaşıldı",
  danger: "Kota Aşıldı",
};

function escapeCsvValue(value) {
  const stringValue = String(value ?? "");

  if (/[",\n]/.test(stringValue)) {
    return `"${stringValue.replace(/"/g, '""')}"`;
  }

  return stringValue;
}

// Dashboard'daki mevcut ev verilerini, backend'e ayrica istek atmadan
// (App.jsx zaten elinde tutuyor) client-side bir CSV dosyasina cevirip
// indirir. Excel/Google Sheets ile dogrudan acilabilir.
export function exportHomesAsCsv(homes) {
  const headerRow = [
    "Ev Adı",
    "Tüketim (kWh)",
    "Fatura (TL)",
    "Kota Kullanımı (%)",
    "Durum",
  ];

  const rows = homes.map((home) => [
    home.name,
    Number(home.consumption || 0).toFixed(2),
    Number(home.bill || 0).toFixed(2),
    Number(home.quotaPercentage || 0).toFixed(0),
    STATUS_LABELS[home.status] || home.status || "",
  ]);

  const csvLines = [headerRow, ...rows].map((row) =>
    row.map(escapeCsvValue).join(",")
  );

  // Basta BOM (﻿) - Excel'in Turkce karakterleri (ı, ş, ğ vb.)
  // doğru göstermesi için gerekli.
  const csvContent = "﻿" + csvLines.join("\r\n");

  const blob = new Blob([csvContent], {
    type: "text/csv;charset=utf-8;",
  });

  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");

  const timestamp = new Date()
    .toLocaleDateString("tr-TR")
    .replaceAll(".", "-");

  link.href = url;
  link.download = `powerpulse-rapor-${timestamp}.csv`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
