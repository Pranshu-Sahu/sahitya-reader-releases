package in.sahitya.reader;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/** Checks and installs APK releases published through a user-configured HTTPS manifest. */
public final class UpdateManager {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final long MAX_MANIFEST_BYTES=256*1024;
    private static final long MAX_APK_BYTES=200L*1024*1024;
    private UpdateManager(){}

    public static final class UpdateInfo {
        public final int versionCode;
        public final String versionName,apkUrl,sha256,releaseNotes;
        UpdateInfo(int code,String name,String url,String hash,String notes){versionCode=code;versionName=name;apkUrl=url;sha256=hash;releaseNotes=notes;}
    }
    public interface CheckCallback { void complete(UpdateInfo update,String error); }
    public interface DownloadCallback { void complete(File apk,String error); }
    public interface InstallCallback { void permissionRequired(File apk);void complete(String error); }

    public static void check(Context context,String manifestUrl,CheckCallback callback){
        WORKER.execute(()->{try{
            HttpURLConnection c=open(manifestUrl,"application/json");
            byte[] bytes=readLimited(c.getInputStream(),MAX_MANIFEST_BYTES);c.disconnect();JSONObject json=new JSONObject(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));
            int version=json.getInt("versionCode");String name=json.optString("versionName",String.valueOf(version));String apk=requiredHttps(json.getString("apkUrl"));String hash=json.getString("sha256").toLowerCase(Locale.ROOT);String notes=json.optString("releaseNotes","");
            if(!hash.matches("[0-9a-f]{64}"))throw new IOException("The update manifest has an invalid SHA-256 checksum.");
            if(version<=BuildConfig.VERSION_CODE)post(()->callback.complete(null,null));else{UpdateInfo info=new UpdateInfo(version,name,apk,hash,notes);post(()->callback.complete(info,null));}
        }catch(Exception e){post(()->callback.complete(null,message(e)));}});
    }
    public static void download(Context context,UpdateInfo info,DownloadCallback callback){
        WORKER.execute(()->{File target=null;try{
            HttpURLConnection c=open(info.apkUrl,"application/vnd.android.package-archive, application/octet-stream");
            int declared=c.getContentLength();if(declared>MAX_APK_BYTES)throw new IOException("The APK is larger than the 200 MB limit.");
            File dir=new File(context.getCacheDir(),"updates");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Could not create a temporary update folder.");target=new File(dir,"sahitya-"+info.versionCode+".apk");
            MessageDigest digest=MessageDigest.getInstance("SHA-256");long total=0;try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(target)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1){total+=n;if(total>MAX_APK_BYTES)throw new IOException("The APK is larger than the 200 MB limit.");digest.update(b,0,n);out.write(b,0,n);}}finally{c.disconnect();}
            String actual=hex(digest.digest());if(!actual.equalsIgnoreCase(info.sha256))throw new IOException("The downloaded APK failed its SHA-256 check.");
            File verified=target;post(()->callback.complete(verified,null));
        }catch(Exception e){if(target!=null)target.delete();post(()->callback.complete(null,message(e)));}});
    }
    public static void install(Activity activity,File apk,InstallCallback callback){
        if(Build.VERSION.SDK_INT>=26&&!activity.getPackageManager().canRequestPackageInstalls()){callback.permissionRequired(apk);return;}
        WORKER.execute(()->{int sessionId=-1;try{
            PackageInstaller installer=activity.getPackageManager().getPackageInstaller();PackageInstaller.SessionParams params=new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);params.setAppPackageName(BuildConfig.APPLICATION_ID);sessionId=installer.createSession(params);
            try(PackageInstaller.Session session=installer.openSession(sessionId)){
                try(InputStream in=new FileInputStream(apk);OutputStream out=session.openWrite("update.apk",0,apk.length())){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)out.write(b,0,n);session.fsync(out);}
                Intent status=new Intent(activity,UpdateInstallReceiver.class).setAction(UpdateInstallReceiver.ACTION_INSTALL_STATUS).putExtra(PackageInstaller.EXTRA_SESSION_ID,sessionId);
                int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=31)flags|=PendingIntent.FLAG_MUTABLE;else if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
                PendingIntent result=PendingIntent.getBroadcast(activity,sessionId,status,flags);session.commit(result.getIntentSender());
            }
            post(()->callback.complete(null));
        }catch(Exception e){if(sessionId!=-1)try{activity.getPackageManager().getPackageInstaller().abandonSession(sessionId);}catch(Exception ignored){}post(()->callback.complete(message(e)));}});
    }

    private static HttpURLConnection open(String address,String accept)throws Exception{
        URL url=new URL(requiredHttps(address));HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setInstanceFollowRedirects(true);c.setRequestMethod("GET");c.setRequestProperty("Accept",accept);c.connect();
        if(!"https".equalsIgnoreCase(c.getURL().getProtocol())){c.disconnect();throw new IOException("Updates must use HTTPS, including after redirects.");}
        int status=c.getResponseCode();if(status<200||status>=300){c.disconnect();throw new IOException("Update server returned HTTP "+status+".");}return c;
    }
    private static String requiredHttps(String value)throws Exception{URL u=new URL(value);if(!"https".equalsIgnoreCase(u.getProtocol())||u.getHost()==null||u.getHost().isEmpty())throw new IOException("Enter a complete HTTPS update URL.");return u.toString();}
    private static byte[] readLimited(InputStream in,long limit)throws IOException{try(InputStream src=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];long total=0;int n;while((n=src.read(b))!=-1){total+=n;if(total>limit)throw new IOException("The update manifest is too large.");out.write(b,0,n);}return out.toByteArray();}}
    private static String hex(byte[] b){StringBuilder s=new StringBuilder(b.length*2);for(byte value:b)s.append(String.format(Locale.ROOT,"%02x",value&0xff));return s.toString();}
    private static String message(Exception e){return e.getMessage()==null?"Could not check this update source.":e.getMessage();}
    private static void post(Runnable r){MAIN.post(r);}
}
