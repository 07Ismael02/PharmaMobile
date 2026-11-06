import os,pathlib,json,re,hashlib
from pypdf import PdfReader
from PIL import Image,ImageDraw
F=pathlib.Path(os.environ['USERPROFILE'])/'AndroidStudioProjects/PharmaMobile'
O=F/'docs/s13-autonoma'; pdf=O/'Logacho_Ismael_Sesion13_Evidencias.pdf';r=PdfReader(str(pdf))
text='\n'.join(p.extract_text() or '' for p in r.pages)
assert len(r.pages)==13
assert pdf.stat().st_size<20_000_000
assert all(abs(float(p.mediabox.width)-612)<.1 and abs(float(p.mediabox.height)-792)<.1 for p in r.pages)
assert 'f0d1e13f0a444d98cd0acb81875d39f6340d02ad' in text
assert '6f6f619fb2c81be10f02491768fc01357592c860' in text
secrets=json.loads((pathlib.Path(os.environ['LOCALAPPDATA'])/'PharmaMobile/S13/secrets.json').read_text(encoding='utf-8-sig'))
for value in secrets.values():assert value not in text
assert not re.search(r'eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}',text)
uris=[]
for page in r.pages:
    for annot in page.get('/Annots',[]):
        a=annot.get_object().get('/A',{})
        if a.get('/URI'):uris.append(str(a['/URI']))
assert len(uris)>=5
qa=pathlib.Path(os.environ['USERPROFILE'])/'Documents/PharmaMobil/tmp/pdfs'
(qa/'texto-extraido.txt').write_text(text,encoding='utf-8')
images=sorted(qa.glob('s13-*.png'))
if len(images)==13:
    sheet=Image.new('RGB',(1040,5*360),'#D5DFDC')
    for i,path in enumerate(images):
        im=Image.open(path).convert('RGB');im.thumbnail((245,325))
        x=(i%4)*260;y=(i//4)*360
        sheet.paste(im,(x+7,y+24));ImageDraw.Draw(sheet).text((x+8,y+4),path.stem,fill='black')
    sheet.crop((0,0,1040,4*360)).save(qa/'contacto-pdf.png')
report={'paginas':len(r.pages),'bytes':pdf.stat().st_size,'formato':'Carta 612 x 792 pt','sha256':hashlib.sha256(pdf.read_bytes()).hexdigest(),'sin_secretos_texto':True,'commits_exactos_presentes':True,'enlaces_internos_pdf':uris,'renderizado':'13 PNG revisados visualmente; ver registro de entrega'}
(O/'verificacion-pdf.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False))
