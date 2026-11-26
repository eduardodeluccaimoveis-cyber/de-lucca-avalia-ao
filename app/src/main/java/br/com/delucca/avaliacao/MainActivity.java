package br.com.delucca.avaliacao;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.print.PrintManager;
import android.webkit.*;
import android.widget.Toast;
import android.view.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
 private static final String HOME="https://appassets.androidplatform.net/index.html";
 private WebView web;
 private ValueCallback<Uri[]> picker;
 private String pending;
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  web=new WebView(this);
  web.setBackgroundColor(0xff142a3a);
  web.setOnApplyWindowInsetsListener((v,insets)->{
   if(android.os.Build.VERSION.SDK_INT>=30){
    android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());
    v.setPadding(i.left,i.top,i.right,i.bottom);
   } else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
   return insets;
  });
  WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(false); s.setAllowContentAccess(true); s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER); s.setJavaScriptCanOpenWindowsAutomatically(false);
  web.setWebViewClient(new WebViewClient(){
   @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){
    if(HOME.equals(r.getUrl().toString())) try {return new WebResourceResponse("text/html","UTF-8",getAssets().open("index.html"));} catch(IOException e){return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));}
    return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",java.util.Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));
   }
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){ return !HOME.equals(r.getUrl().toString()); }
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> cb,FileChooserParams p){
    if(picker!=null)picker.onReceiveValue(null); picker=cb;
    try{startActivityForResult(p.createIntent(),100);}catch(Exception e){picker.onReceiveValue(null);picker=null;toast("Não foi possível selecionar arquivo.");} return true;
   }
   @Override public boolean onJsPrompt(WebView w,String url,String message,String value,JsPromptResult result){
    if(!HOME.equals(url)){result.cancel();return true;}
    try{
     switch(message){
      case "DL_PRINT": ((PrintManager)getSystemService(PRINT_SERVICE)).print("Avaliação Imobiliária De Lucca",web.createPrintDocumentAdapter("Avaliação Imobiliária De Lucca"),new android.print.PrintAttributes.Builder().build()); break;
      case "DL_EXPORT": pending=value; Intent save=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE,"avaliacoes-de-lucca.json"); startActivityForResult(save,101);break;
      case "DL_OPEN": Uri u=Uri.parse(value);if(!"https".equals(u.getScheme())||!"www.google.com".equals(u.getHost())||!"/search".equals(u.getPath())||u.getUserInfo()!=null){result.cancel();return true;}startActivity(new Intent(Intent.ACTION_VIEW,u));break;
      default:return super.onJsPrompt(w,url,message,value,result);
     } result.confirm("ok");
    }catch(Exception e){toast("Não foi possível concluir esta ação.");result.cancel();}return true;
   }
  });
  setContentView(web);web.requestApplyInsets();web.loadUrl(HOME);
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
  if(request==100&&picker!=null){ValueCallback<Uri[]> cb=picker;picker=null;cb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));}
  if(request==101){String json=pending;pending=null;if(result==RESULT_OK&&data!=null&&data.getData()!=null&&json!=null){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new IOException();out.write(json.getBytes(StandardCharsets.UTF_8));toast("Backup salvo.");}catch(Exception e){toast("Não foi possível salvar o backup.");}}}
 }
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 @Override protected void onDestroy(){if(picker!=null){picker.onReceiveValue(null);picker=null;}if(web!=null)web.destroy();super.onDestroy();}
}
