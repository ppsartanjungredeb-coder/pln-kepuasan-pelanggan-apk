const TZ = 'Asia/Makassar';
const SHEETS = {
  DASHBOARD: 'DASHBOARD', DATABASE: 'DATABASE_SURVEI', DAILY: 'REKAP_HARIAN',
  MONTHLY: 'REKAP_BULANAN', YEARLY: 'REKAP_TAHUNAN', CONFIG: 'PENGATURAN'
};

function doGet() {
  return HtmlService.createHtmlOutputFromFile('Index')
    .setTitle('Kepuasan Pelanggan - PLN ULP Tanjung Redeb')
    .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL);
}

function doPost(e) {
  try {
    const data = JSON.parse((e && e.postData && e.postData.contents) || '{}');
    const result = saveSurvey(data);
    return ContentService.createTextOutput(JSON.stringify(result)).setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ok:false, error:String(err)})).setMimeType(ContentService.MimeType.JSON);
  }
}

function getSS_() {
  const id = PropertiesService.getScriptProperties().getProperty('SPREADSHEET_ID');
  if (id) return SpreadsheetApp.openById(id);
  const active = SpreadsheetApp.getActiveSpreadsheet();
  if (!active) throw new Error('Spreadsheet belum dikaitkan. Jalankan setupSpreadsheet() dari Apps Script yang terikat ke Google Sheet.');
  PropertiesService.getScriptProperties().setProperty('SPREADSHEET_ID', active.getId());
  return active;
}

function setupSpreadsheet() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  if (!ss) throw new Error('Buka Apps Script melalui Extensions > Apps Script dari Google Spreadsheet.');
  PropertiesService.getScriptProperties().setProperty('SPREADSHEET_ID', ss.getId());

  const names = Object.values(SHEETS);
  names.forEach(name => { if (!ss.getSheetByName(name)) ss.insertSheet(name); });

  setupDatabase_(ss.getSheetByName(SHEETS.DATABASE));
  setupConfig_(ss.getSheetByName(SHEETS.CONFIG));
  setupRecapSheet_(ss.getSheetByName(SHEETS.DAILY), 'REKAP KEPUASAN HARIAN', ['Tanggal','Sangat Baik','Baik','Buruk','Sangat Buruk','Total Responden','Nilai Rata-rata','Persentase Kepuasan']);
  setupRecapSheet_(ss.getSheetByName(SHEETS.MONTHLY), 'REKAP KEPUASAN BULANAN', ['Bulan','Tahun','Sangat Baik','Baik','Buruk','Sangat Buruk','Total Responden','Nilai Rata-rata','Persentase Kepuasan']);
  setupRecapSheet_(ss.getSheetByName(SHEETS.YEARLY), 'REKAP KEPUASAN TAHUNAN', ['Tahun','Sangat Baik','Baik','Buruk','Sangat Buruk','Total Responden','Nilai Rata-rata','Persentase Kepuasan']);
  setupDashboard_(ss.getSheetByName(SHEETS.DASHBOARD));
  rebuildRecaps_();
  SpreadsheetApp.flush();
  return 'Setup selesai. Spreadsheet siap digunakan.';
}

function setupDatabase_(sh) {
  sh.clear();
  const headers = ['No','Timestamp','Tanggal','Waktu','Hari','Bulan','Tahun','Rating','Kategori','Skor','Device','Lokasi','Loket','Sync ID'];
  sh.getRange(1,1,1,headers.length).setValues([headers]).setBackground('#0072BC').setFontColor('#FFFFFF').setFontWeight('bold').setHorizontalAlignment('center');
  sh.setFrozenRows(1);
  sh.getRange('B:B').setNumberFormat('dd/MM/yyyy HH:mm:ss');
  sh.getRange('C:C').setNumberFormat('dd/MM/yyyy');
  sh.getRange('D:D').setNumberFormat('HH:mm:ss');
  [8,21,14,12,12,14,10,10,18,10,16,25,20,28].forEach((w,i)=>sh.setColumnWidth(i+1,w*7));
}

function setupConfig_(sh) {
  sh.clear();
  sh.getRange('A1:E2').merge().setValue('PENGATURAN APLIKASI KEPUASAN PELANGGAN').setBackground('#005B96').setFontColor('#FFFFFF').setFontWeight('bold').setFontSize(15).setHorizontalAlignment('center').setVerticalAlignment('middle');
  const data = [
    ['Parameter','Nilai'],['Nama Unit','PLN ULP Tanjung Redeb'],['Lokasi','Kantor PLN ULP Tanjung Redeb'],['Device ID','TABLET-01'],['Nama Loket','Ruang Pelayanan'],
    ['Judul Survei','Bagaimana Pelayanan Kami Hari Ini?'],['Status Aplikasi','AKTIF'],['Target Kepuasan',0.95],['Jam Buka','08:00'],['Jam Tutup','16:00']
  ];
  sh.getRange(4,1,data.length,2).setValues(data);
  sh.getRange('A4:B4').setBackground('#00A9CE').setFontColor('#FFFFFF').setFontWeight('bold');
  sh.getRange('A5:A13').setFontWeight('bold');
  sh.getRange('B11').setNumberFormat('0.00%');
  sh.getRange('D4:E8').setValues([['Kategori','Skor'],['SANGAT BAIK',4],['BAIK',3],['BURUK',2],['SANGAT BURUK',1]]);
  sh.getRange('D4:E4').setBackground('#0072BC').setFontColor('#FFFFFF').setFontWeight('bold');
  sh.setColumnWidths(1,2,220); sh.setColumnWidths(4,2,150);
}

function setupRecapSheet_(sh, title, headers) {
  sh.clear();
  sh.getRange(1,1,2,headers.length).merge().setValue(title).setBackground('#005B96').setFontColor('#FFFFFF').setFontWeight('bold').setFontSize(15).setHorizontalAlignment('center').setVerticalAlignment('middle');
  sh.getRange(4,1,1,headers.length).setValues([headers]).setBackground('#0072BC').setFontColor('#FFFFFF').setFontWeight('bold').setHorizontalAlignment('center').setWrap(true);
  sh.setFrozenRows(4);
  sh.setColumnWidths(1,headers.length,135);
}

function setupDashboard_(sh) {
  sh.clear(); sh.getCharts().forEach(c=>sh.removeChart(c));
  sh.getRange('A1:L2').merge().setValue('DASHBOARD KEPUASAN PELANGGAN').setBackground('#005B96').setFontColor('#FFFFFF').setFontWeight('bold').setFontSize(20).setHorizontalAlignment('center').setVerticalAlignment('middle');
  sh.getRange('A3:L3').merge().setValue('PLN ULP TANJUNG REDEB • Database utama: Spreadsheet').setBackground('#F5FAFD').setFontColor('#6B7280').setHorizontalAlignment('center');

  const cards = [
    ['A5:C5','A6:C8','RESPONDEN HARI INI','=COUNTIFS(DATABASE_SURVEI!$C$2:$C,TODAY())','#0072BC','0'],
    ['D5:F5','D6:F8','RESPONDEN BULAN INI','=COUNTIFS(DATABASE_SURVEI!$C$2:$C,">="&EOMONTH(TODAY(),-1)+1,DATABASE_SURVEI!$C$2:$C,"<="&EOMONTH(TODAY(),0))','#00A9CE','0'],
    ['G5:I5','G6:I8','KEPUASAN HARI INI','=IFERROR((COUNTIFS(DATABASE_SURVEI!$C$2:$C,TODAY(),DATABASE_SURVEI!$I$2:$I,"SANGAT BAIK")+COUNTIFS(DATABASE_SURVEI!$C$2:$C,TODAY(),DATABASE_SURVEI!$I$2:$I,"BAIK"))/COUNTIFS(DATABASE_SURVEI!$C$2:$C,TODAY()),0)','#00A99D','0.00%'],
    ['J5:L5','J6:L8','RATA-RATA HARI INI','=IFERROR(SUMIFS(DATABASE_SURVEI!$J$2:$J,DATABASE_SURVEI!$C$2:$C,TODAY())/COUNTIFS(DATABASE_SURVEI!$C$2:$C,TODAY()),0)','#FDB813','0.00']
  ];
  cards.forEach(c=>{
    sh.getRange(c[0]).merge().setValue(c[2]).setBackground(c[4]).setFontColor('#FFFFFF').setFontWeight('bold').setHorizontalAlignment('center').setVerticalAlignment('middle');
    sh.getRange(c[1]).merge().setFormula(c[3]).setFontSize(22).setFontWeight('bold').setHorizontalAlignment('center').setVerticalAlignment('middle').setNumberFormat(c[5]);
  });

  sh.getRange('A10:C10').merge().setValue('RINGKASAN HARI INI').setBackground('#0072BC').setFontColor('#FFFFFF').setFontWeight('bold').setHorizontalAlignment('center');
  sh.getRange('A11:C15').setValues([['Emot','Kategori','Jumlah'],['😄','SANGAT BAIK',''],['🙂','BAIK',''],['☹️','BURUK',''],['😡','SANGAT BURUK','']]);
  sh.getRange('A11:C11').setBackground('#00A9CE').setFontColor('#FFFFFF').setFontWeight('bold');
  for (let r=12;r<=15;r++) sh.getRange(r,3).setFormula(`=COUNTIFS(DATABASE_SURVEI!$C$2:$C,TODAY(),DATABASE_SURVEI!$I$2:$I,B${r})`);
  sh.getRange('A12:A15').setFontSize(18);

  sh.getRange('A17:C17').merge().setValue('TARGET & ALERT').setBackground('#005B96').setFontColor('#FFFFFF').setFontWeight('bold').setHorizontalAlignment('center');
  sh.getRange('A18:B21').setValues([['Indikator','Nilai'],['Target Kepuasan',''],['Realisasi Hari Ini',''],['Buruk + Sangat Buruk','']]);
  sh.getRange('A18:B18').setBackground('#00A9CE').setFontColor('#FFFFFF').setFontWeight('bold');
  sh.getRange('B19').setFormula('=PENGATURAN!B11').setNumberFormat('0.00%');
  sh.getRange('B20').setFormula('=G6').setNumberFormat('0.00%');
  sh.getRange('B21').setFormula('=C14+C15');

  // Helper tables for charts.
  sh.getRange('N1:O5').setValues([['Kategori','Jumlah'],['SANGAT BAIK',''],['BAIK',''],['BURUK',''],['SANGAT BURUK','']]);
  for (let r=2;r<=5;r++) sh.getRange(r,15).setFormula(`=C${r+10}`);
  sh.getRange('Q1:R3').setValues([['Status','Jumlah'],['Puas',''],['Kurang Puas','']]);
  sh.getRange('R2').setFormula('=C12+C13'); sh.getRange('R3').setFormula('=C14+C15');
  sh.getRange('T1:U16').clearContent();
  sh.getRange('T1:U1').setValues([['Tanggal','Kepuasan']]);
  sh.getRange('T2').setFormula('=IFERROR(INDEX(REKAP_HARIAN!A5:A,MAX(1,COUNTA(REKAP_HARIAN!A5:A)-13)),"")');
  for (let r=3;r<=15;r++) sh.getRange(r,20).setFormula(`=IF(T${r-1}="","",T${r-1}+1)`);
  for (let r=2;r<=15;r++) sh.getRange(r,21).setFormula(`=IFERROR(VLOOKUP(T${r},REKAP_HARIAN!A:H,8,FALSE),"")`);
  sh.getRange('W1:X13').clearContent(); sh.getRange('W1:X1').setValues([['Bulan','Kepuasan']]);
  for (let r=2;r<=13;r++){ sh.getRange(r,23).setFormula(`=IFERROR(INDEX(REKAP_BULANAN!A$5:A,${r-1}),"")`); sh.getRange(r,24).setFormula(`=IFERROR(INDEX(REKAP_BULANAN!I$5:I,${r-1}),"")`); }

  const c1 = sh.newChart().asPieChart().addRange(sh.getRange('N1:O5')).setOption('title','Distribusi Kepuasan Hari Ini').setOption('pieHole',0.35).setPosition(10,4,0,0).build();
  const c2 = sh.newChart().asPieChart().addRange(sh.getRange('Q1:R3')).setOption('title','Persentase Kepuasan Hari Ini').setOption('pieHole',0.65).setPosition(10,9,0,0).build();
  const c3 = sh.newChart().asLineChart().addRange(sh.getRange('T1:U15')).setOption('title','Tren Kepuasan Harian').setPosition(27,1,0,0).build();
  const c4 = sh.newChart().asColumnChart().addRange(sh.getRange('W1:X13')).setOption('title','Rekap Kepuasan Bulanan').setPosition(27,7,0,0).build();
  [c1,c2,c3,c4].forEach(c=>sh.insertChart(c));
  sh.hideColumns(14,11);
  sh.setFrozenRows(3); sh.setColumnWidths(1,12,92);
}

function saveSurvey(data) {
  validatePayload_(data);
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const ss = getSS_();
    const sh = ss.getSheetByName(SHEETS.DATABASE);
    if (!sh) throw new Error('Sheet DATABASE_SURVEI belum ada. Jalankan setupSpreadsheet().');

    // Idempotensi: cegah sync ID yang sama tersimpan dua kali.
    if (data.syncId) {
      const hit = sh.getRange('N:N').createTextFinder(String(data.syncId)).matchEntireCell(true).findNext();
      if (hit) return {ok:true, duplicate:true};
    }

    const now = new Date();
    const hari = ['Minggu','Senin','Selasa','Rabu','Kamis','Jumat','Sabtu'][Number(Utilities.formatDate(now,TZ,'u')) % 7];
    const bulan = ['Januari','Februari','Maret','April','Mei','Juni','Juli','Agustus','September','Oktober','November','Desember'][Number(Utilities.formatDate(now,TZ,'M'))-1];
    const tanggal = new Date(Utilities.formatDate(now,TZ,'yyyy/MM/dd') + ' 00:00:00');
    const waktu = Utilities.formatDate(now,TZ,'HH:mm:ss');
    const no = Math.max(1, sh.getLastRow());

    sh.appendRow([no, now, tanggal, waktu, hari, bulan, Number(Utilities.formatDate(now,TZ,'yyyy')), data.rating, data.category, Number(data.score), data.device || '', data.location || '', data.counter || '', data.syncId || Utilities.getUuid()]);
    sh.getRange(sh.getLastRow(),2).setNumberFormat('dd/MM/yyyy HH:mm:ss');
    sh.getRange(sh.getLastRow(),3).setNumberFormat('dd/MM/yyyy');

    rebuildRecaps_();
    return {ok:true};
  } finally {
    lock.releaseLock();
  }
}

function validatePayload_(d) {
  const allowed = {'SANGAT BAIK':4,'BAIK':3,'BURUK':2,'SANGAT BURUK':1};
  if (!d || !(d.category in allowed)) throw new Error('Kategori penilaian tidak valid.');
  if (Number(d.score) !== allowed[d.category]) throw new Error('Skor tidak valid.');
}

function rebuildRecaps_() {
  const ss = getSS_();
  const db = ss.getSheetByName(SHEETS.DATABASE);
  const last = db.getLastRow();
  const rows = last > 1 ? db.getRange(2,1,last-1,14).getValues() : [];
  const daily = {}, monthly = {}, yearly = {};

  rows.forEach(r => {
    if (!r[2] || !r[8]) return;
    const dt = new Date(r[2]);
    const y = Number(r[6]);
    const mIdx = dt.getMonth();
    const dKey = Utilities.formatDate(dt,TZ,'yyyy-MM-dd');
    const mKey = `${y}-${String(mIdx+1).padStart(2,'0')}`;
    const yKey = String(y);
    addAgg_(daily,dKey,r[8],Number(r[9]),dt,y,mIdx);
    addAgg_(monthly,mKey,r[8],Number(r[9]),dt,y,mIdx);
    addAgg_(yearly,yKey,r[8],Number(r[9]),dt,y,mIdx);
  });

  writeDaily_(ss.getSheetByName(SHEETS.DAILY), daily);
  writeMonthly_(ss.getSheetByName(SHEETS.MONTHLY), monthly);
  writeYearly_(ss.getSheetByName(SHEETS.YEARLY), yearly);
}

function addAgg_(obj,key,cat,score,date,year,monthIdx){
  if(!obj[key]) obj[key]={date,year,monthIdx,sb:0,b:0,buruk:0,sburuk:0,total:0,sum:0};
  const a=obj[key];
  if(cat==='SANGAT BAIK')a.sb++; else if(cat==='BAIK')a.b++; else if(cat==='BURUK')a.buruk++; else if(cat==='SANGAT BURUK')a.sburuk++;
  a.total++; a.sum+=score;
}
function calc_(a){ return {avg:a.total?a.sum/a.total:0, sat:a.total?(a.sb+a.b)/a.total:0}; }
function monthName_(i){return ['Januari','Februari','Maret','April','Mei','Juni','Juli','Agustus','September','Oktober','November','Desember'][i];}

function clearRecapBody_(sh, cols){ const lr=sh.getLastRow(); if(lr>=5) sh.getRange(5,1,Math.max(1,lr-4),cols).clearContent(); }
function writeDaily_(sh,obj){
  clearRecapBody_(sh,8); const keys=Object.keys(obj).sort(); const out=keys.map(k=>{const a=obj[k],c=calc_(a);return [a.date,a.sb,a.b,a.buruk,a.sburuk,a.total,c.avg,c.sat];});
  if(out.length){sh.getRange(5,1,out.length,8).setValues(out);sh.getRange(5,1,out.length,1).setNumberFormat('dd/MM/yyyy');sh.getRange(5,7,out.length,1).setNumberFormat('0.00');sh.getRange(5,8,out.length,1).setNumberFormat('0.00%');}
}
function writeMonthly_(sh,obj){
  clearRecapBody_(sh,9); const keys=Object.keys(obj).sort(); const out=keys.map(k=>{const a=obj[k],c=calc_(a);return [monthName_(a.monthIdx),a.year,a.sb,a.b,a.buruk,a.sburuk,a.total,c.avg,c.sat];});
  if(out.length){sh.getRange(5,1,out.length,9).setValues(out);sh.getRange(5,8,out.length,1).setNumberFormat('0.00');sh.getRange(5,9,out.length,1).setNumberFormat('0.00%');}
}
function writeYearly_(sh,obj){
  clearRecapBody_(sh,8); const keys=Object.keys(obj).sort(); const out=keys.map(k=>{const a=obj[k],c=calc_(a);return [a.year,a.sb,a.b,a.buruk,a.sburuk,a.total,c.avg,c.sat];});
  if(out.length){sh.getRange(5,1,out.length,8).setValues(out);sh.getRange(5,7,out.length,1).setNumberFormat('0.00');sh.getRange(5,8,out.length,1).setNumberFormat('0.00%');}
}

function getPublicConfig(){
  const ss=getSS_(); const sh=ss.getSheetByName(SHEETS.CONFIG); if(!sh)return {};
  return {unit:sh.getRange('B5').getDisplayValue(),location:sh.getRange('B6').getDisplayValue(),device:sh.getRange('B7').getDisplayValue(),counter:sh.getRange('B8').getDisplayValue(),title:sh.getRange('B9').getDisplayValue()};
}
