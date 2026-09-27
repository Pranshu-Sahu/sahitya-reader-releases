package in.sahitya.reader;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PICK_DOCUMENT = 17;
    private static final int REQUEST_INSTALL_PERMISSION = 19;
    private static final String DEFAULT_UPDATE_MANIFEST_URL="https://raw.githubusercontent.com/Pranshu-Sahu/sahitya-reader-releases/main/update-manifest.json";
    private LibraryDb db; private LinearLayout list; private String filter="All"; private boolean dark;
    private java.io.File pendingUpdateApk;
    private final int ink=0xff26372f, green=0xff315d48, muted=0xff778078, paper=0xfff7f5ef;
    @Override public void onCreate(Bundle b) { super.onCreate(b); db=new LibraryDb(this); ensureCompleteGodaan(); android.content.SharedPreferences prefs=getSharedPreferences("reader_preferences",MODE_PRIVATE);dark=prefs.getBoolean("dark",false);String pending=prefs.getString("pending_update_apk","");if(!pending.isEmpty()){File f=new File(pending);if(f.isFile())pendingUpdateApk=f;}render(); }
    @Override protected void onResume(){super.onResume();if(db!=null){dark=getSharedPreferences("reader_preferences",MODE_PRIVATE).getBoolean("dark",false);render();}}
    private int bg() { return dark ? 0xff171b18 : paper; }
    private int card() { return dark ? 0xff222924 : 0xffffffff; }
    private int fg() { return dark ? 0xfff0eee5 : ink; }
    private TextView text(String s,int size,int color) { TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t; }
    private void render() {
        getWindow().setStatusBarColor(bg());getWindow().setNavigationBarColor(bg());int systemFlags=dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;if(!dark&&android.os.Build.VERSION.SDK_INT>=26)systemFlags|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;getWindow().getDecorView().setSystemUiVisibility(systemFlags);
        android.widget.ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(bg());
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(24),dp(20),dp(24),dp(28)); scroll.addView(root); setContentView(scroll);
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=text("S A H I T Y A     /     READER",12,green); brand.setTypeface(null,Typeface.BOLD); top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView update=text("↻",20,green);update.setGravity(Gravity.CENTER);update.setContentDescription("Check for app updates");LinearLayout.LayoutParams up=new LinearLayout.LayoutParams(dp(42),dp(42));top.addView(update,up);update.setOnClickListener(v->checkOrConfigureUpdates());update.setOnLongClickListener(v->{configureUpdates();return true;});
        LinearLayout theme=button(dark?"☀":"☾", false); LinearLayout.LayoutParams thp=new LinearLayout.LayoutParams(dp(42),dp(42));thp.leftMargin=dp(8);top.addView(theme,thp); theme.setOnClickListener(v->{dark=!dark;getSharedPreferences("reader_preferences",MODE_PRIVATE).edit().putBoolean("dark",dark).apply();render();}); root.addView(top);
        TextView hello=text("A quieter place\nto read.",32,fg()); hello.setTypeface(null,Typeface.BOLD); hello.setPadding(0,dp(22),0,dp(5)); root.addView(hello);
        TextView sub=text("Your books and study notes, together.",15,muted); root.addView(sub);
        LinearLayout add=button("＋   Add a book or document",true); LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(54));ap.topMargin=dp(22);root.addView(add,ap);add.setOnClickListener(v->pick());
        TextView heading=text("YOUR LIBRARY",11,muted);heading.setTypeface(null,Typeface.BOLD);heading.setPadding(0,dp(28),0,dp(10));root.addView(heading);
        HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout chips=new LinearLayout(this);String[] filters={"All","Godaan","Polity","History","Other"};
        for(String f:filters){TextView chip=text(f,13,f.equals(filter)?0xffffffff:fg());chip.setPadding(dp(16),dp(10),dp(16),dp(10));chip.setBackground(round(f.equals(filter)?green:card(),24));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,-2);cp.rightMargin=dp(8);chips.addView(chip,cp);chip.setOnClickListener(v->{filter=f;render();});}
        hs.addView(chips);root.addView(hs);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(14);root.addView(list,lp); populate();
        TextView tip=text("TXT, EPUB and PDF  ·  Stored on this device",12,muted);tip.setGravity(Gravity.CENTER);tip.setPadding(0,dp(24),0,0);root.addView(tip);
    }
    private void populate(){List<Book> books=db.books(filter);if(books.isEmpty()){TextView empty=text("Your reading list starts here.\nAdd a file you own to begin.",15,muted);empty.setGravity(Gravity.CENTER);empty.setPadding(0,dp(40),0,dp(40));list.addView(empty);return;}
        for(Book b:books){LinearLayout tile=new LinearLayout(this);tile.setOrientation(LinearLayout.VERTICAL);tile.setPadding(dp(18),dp(16),dp(18),dp(15));tile.setBackground(round(card(),18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(10);list.addView(tile,p);
            TextView meta=text(b.subject.toUpperCase(Locale.ROOT)+"     ·     "+b.language+"     ·     "+b.format,10,muted);meta.setTypeface(null,Typeface.BOLD);tile.addView(meta);
            TextView title=text(b.title,20,fg());title.setTypeface(null,Typeface.BOLD);title.setPadding(0,dp(7),0,dp(8));tile.addView(title);
            ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgress(b.progress);bar.setProgressTintList(android.content.res.ColorStateList.valueOf(green));bar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(dark?0xff3b443e:0xffe9e8e0));tile.addView(bar,new LinearLayout.LayoutParams(-1,dp(3)));
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView progress=text(b.progress==0?"Not started":b.progress+"% read",12,muted);row.addView(progress,new LinearLayout.LayoutParams(0,-2,1));TextView open=text("Continue  →",13,green);open.setTypeface(null,Typeface.BOLD);row.addView(open);row.setPadding(0,dp(11),0,0);tile.addView(row);
            tile.setOnClickListener(v->open(b.id,null));tile.setOnLongClickListener(v->{if(isBundledGodaan(b)){new AlertDialog.Builder(this).setTitle("About this edition").setMessage(godaanEditionNotes()).setPositiveButton("Done",null).show();return true;}new AlertDialog.Builder(this).setTitle(b.title).setItems(new String[]{"Open","Delete from library"},(d,w)->{if(w==0)open(b.id,null);else new AlertDialog.Builder(this).setMessage("Remove this item from your library?").setNegativeButton("Cancel",null).setPositiveButton("Remove",(x,y)->{db.deleteBook(b.id);new File(b.path).delete();render();}).show();}).show();return true;});
        }
    }
    private void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/plain","application/epub+zip","application/pdf","application/octet-stream"});i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_DOCUMENT);}
    private void checkOrConfigureUpdates(){if(pendingUpdateApk!=null&&pendingUpdateApk.isFile()){installDownloadedUpdate(pendingUpdateApk);return;}String url=getSharedPreferences("reader_preferences",MODE_PRIVATE).getString("update_manifest_url",DEFAULT_UPDATE_MANIFEST_URL);checkUpdates(url);}
    private void configureUpdates(){EditText input=new EditText(this);input.setSingleLine(true);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);input.setHint("https://…/update-manifest.json");String saved=getSharedPreferences("reader_preferences",MODE_PRIVATE).getString("update_manifest_url","");input.setText(saved.isEmpty()?DEFAULT_UPDATE_MANIFEST_URL:saved);new AlertDialog.Builder(this).setTitle("App update source").setMessage("Use the update-manifest.json link, not an APK download link. Long press the update icon any time to change it.").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save & check",(d,w)->{String url=input.getText().toString().trim();getSharedPreferences("reader_preferences",MODE_PRIVATE).edit().putString("update_manifest_url",url).apply();checkUpdates(url);}).show();}
    private void ensureCompleteGodaan(){
        File library=new File(getFilesDir(),"library");File target=new File(library,"godaan-chapters");
        if(!target.exists()&&!target.mkdirs())return;
        try{
            android.content.SharedPreferences preferences=getSharedPreferences("reader_preferences",MODE_PRIVATE);
            boolean refreshText=preferences.getInt("godaan_content_revision",0)<2;
            for(int chapter=1;chapter<=36;chapter++){
                String name=String.format(Locale.ROOT,"%02d.txt",chapter);File page=new File(target,name);
                if(refreshText||!page.isFile()||page.length()==0){try(InputStream in=getAssets().open("godaan-chapters/"+name);OutputStream out=new FileOutputStream(page)){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}}
            }
            preferences.edit().putInt("godaan_content_revision",2).apply();
            String path=target.getAbsolutePath();String title="गोदान — प्रेमचंद (संपूर्ण उपन्यास)";
            File oldPdf=new File(library,"godaan-complete-hi.pdf");File oldDraft=new File(library,"godaan-first-100-pages-hi.txt");
            if(db.hasBookPath(oldPdf.getAbsolutePath()))db.replaceBundledBook(oldPdf.getAbsolutePath(),path,title);
            if(!db.hasBookPath(path))db.addBook(title,"Godaan","Hindi",path,"CHAPTERS");
            db.deleteBookByPath(oldDraft.getAbsolutePath());
            if(oldPdf.isFile())oldPdf.delete();if(oldDraft.isFile())oldDraft.delete();
        }catch(IOException e){Toast.makeText(this,"Complete Godaan could not be added to the library.",Toast.LENGTH_LONG).show();}
    }
    private boolean isBundledGodaan(Book book){return book.path.endsWith(File.separator+"godaan-chapters");}
    private String godaanEditionNotes(){try(InputStream in=getAssets().open("godaan-sources-and-qc.md");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);}catch(IOException e){return "Complete Hindi reading edition from Wikisource, presented in 36 chapters.";}}
    private void checkUpdates(String url){if(!url.startsWith("https://")){new AlertDialog.Builder(this).setTitle("HTTPS required").setMessage("Update sources must use a secure HTTPS URL.").setPositiveButton("OK",null).show();return;}Toast.makeText(this,"Checking for updates…",Toast.LENGTH_SHORT).show();UpdateManager.check(this,url,(info,error)->{if(error!=null){new AlertDialog.Builder(this).setTitle("Update check failed").setMessage(error).setPositiveButton("OK",null).show();return;}if(info==null){new AlertDialog.Builder(this).setTitle("You’re up to date").setMessage("Sahitya Reader "+BuildConfig.VERSION_NAME+" is the latest version on this update source.").setPositiveButton("OK",null).show();return;}downloadUpdate(info);});}
    private void downloadUpdate(UpdateManager.UpdateInfo info){Toast.makeText(this,"Downloading update…",Toast.LENGTH_LONG).show();UpdateManager.download(this,info,(apk,error)->{if(error!=null){new AlertDialog.Builder(this).setTitle("Download failed").setMessage(error).setPositiveButton("OK",null).show();return;}pendingUpdateApk=apk;installDownloadedUpdate(apk);});}
    private void installDownloadedUpdate(java.io.File apk){UpdateManager.install(this,apk,new UpdateManager.InstallCallback(){@Override public void permissionRequired(java.io.File file){pendingUpdateApk=file;getSharedPreferences("reader_preferences",MODE_PRIVATE).edit().putString("pending_update_apk",file.getAbsolutePath()).apply();Intent settings=new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName()));startActivityForResult(settings,REQUEST_INSTALL_PERMISSION);}@Override public void complete(String error){if(error!=null)new AlertDialog.Builder(MainActivity.this).setTitle("Could not start installer").setMessage(error).setPositiveButton("OK",null).show();else{pendingUpdateApk=null;getSharedPreferences("reader_preferences",MODE_PRIVATE).edit().remove("pending_update_apk").apply();Toast.makeText(MainActivity.this,"Continue in Android’s installer to apply the update.",Toast.LENGTH_LONG).show();}}});}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==REQUEST_INSTALL_PERMISSION){if(pendingUpdateApk!=null&&android.os.Build.VERSION.SDK_INT>=26&&getPackageManager().canRequestPackageInstalls())installDownloadedUpdate(pendingUpdateApk);else Toast.makeText(this,"Allow Sahitya Reader to install this update, then tap the update icon to continue.",Toast.LENGTH_LONG).show();return;}if(req!=PICK_DOCUMENT||result!=RESULT_OK||data==null)return;Uri uri=data.getData();String name="Reading";try{android.database.Cursor c=getContentResolver().query(uri,null,null,null,null);if(c!=null){if(c.moveToFirst())name=c.getString(c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME));c.close();}}catch(Exception ignored){}
        String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT):"";String format=ext.equals("pdf")?"PDF":ext.equals("epub")?"EPUB":ext.equals("txt")?"TXT":"";if(format.isEmpty()){new AlertDialog.Builder(this).setMessage("Choose a TXT, EPUB or PDF file.").setPositiveButton("OK",null).show();return;}
        final String filename=name;final String fmt=format;final String[] subjects={"Godaan","Polity","History","Other"};final String[] langs={"Hindi","English","Bilingual","Unknown"};final android.widget.LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);fields.setPadding(dp(22),dp(6),dp(22),0);EditText title=new EditText(this);title.setSingleLine(true);title.setText(filename.replaceFirst("(?i)\\.[^.]+$",""));title.setHint("Title");fields.addView(title);Spinner sub=spinner(subjects);fields.addView(sub);Spinner lang=spinner(langs);fields.addView(lang);
        new AlertDialog.Builder(this).setTitle("Add to your library").setView(fields).setNegativeButton("Cancel",null).setPositiveButton("Add",(d,w)->{try{File dir=new File(getFilesDir(),"library");dir.mkdirs();File target=new File(dir,System.currentTimeMillis()+"."+ext);try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(target)){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))>0)out.write(buffer,0,n);}db.addBook(title.getText().toString().trim().isEmpty()?filename:title.getText().toString().trim(),subjects[sub.getSelectedItemPosition()],langs[lang.getSelectedItemPosition()],target.getAbsolutePath(),fmt);filter="All";render();}catch(Exception e){new AlertDialog.Builder(this).setMessage("Could not import this file: "+e.getMessage()).setPositiveButton("OK",null).show();}}).show();
    }
    private Spinner spinner(String[] a){Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,a));return s;}
    private void open(long id,Long pair){Intent i=new Intent(this,ReaderActivity.class);i.putExtra("book",id);if(pair!=null)i.putExtra("pair",pair);startActivity(i);}
    private LinearLayout button(String label,boolean primary){LinearLayout b=new LinearLayout(this);b.setGravity(Gravity.CENTER);b.setBackground(round(primary?green:card(),14));TextView t=text(label,primary?15:20,primary?0xffffffff:fg());if(primary)t.setTypeface(null,Typeface.BOLD);b.addView(t);return b;}
    private android.graphics.drawable.GradientDrawable round(int color,int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
}
