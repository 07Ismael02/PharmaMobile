import subprocess, os, json, sqlite3, pathlib, xml.etree.ElementTree as ET, sys, datetime
adb=str(pathlib.Path(os.environ['LOCALAPPDATA'])/'Android/Sdk/platform-tools/adb.exe')
user=sys.argv[1]; label=sys.argv[2]
base=pathlib.Path(os.environ['USERPROFILE'])/'AndroidStudioProjects/PharmaMobile/docs/s13-autonoma/evidencias/tecnica'
def run(*args):
    return subprocess.run([adb,*args],capture_output=True,check=False)
prefs=run('exec-out','run-as','com.edu.pe','--user',user,'cat','shared_prefs/sesion_segura.xml')
result={'fecha_host_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'android_user':int(user)}
if prefs.returncode==0 and prefs.stdout.lstrip().startswith(b'<?xml'):
    root=ET.fromstring(prefs.stdout)
    result['almacen']={n.attrib['name']:{'presente':True,'longitud_cifrado':len(n.text or ''),'formato_iv_cifrado':':' in (n.text or '')} for n in root if n.attrib.get('name') in ['token','identidad_validada','cuenta_vinculada']}
else: result['almacen']={}
dbnames=run('shell','run-as','com.edu.pe','--user',user,'ls','databases').stdout.decode().splitlines()
result['bases']=len([n for n in dbnames if n.endswith('.db')])
for name in dbnames:
    if not name.endswith('.db'): continue
    data=run('exec-out','run-as','com.edu.pe','--user',user,'cat','databases/'+name)
    local=pathlib.Path(os.environ['LOCALAPPDATA'])/'PharmaMobile/S13'/('snapshot-'+user+'.db')
    local.write_bytes(data.stdout)
    con=sqlite3.connect(str(local));con.row_factory=sqlite3.Row
    result['productos']=[dict(r) for r in con.execute('select id,nombre,pendiente,intentoEnvio,intentos,ultimoError from producto order by id')]
    result['estado_sync']=[dict(r) for r in con.execute('select * from estado_sync')]
    result['categorias']=[dict(r) for r in con.execute('select * from categoria')]
    con.close()
path=base/(label+'.json'); path.write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=False))
