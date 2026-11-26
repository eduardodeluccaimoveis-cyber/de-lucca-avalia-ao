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
import java.net.*;
import org.json.*;
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
  WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(false); s.setAllowContentAccess(true); s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW); s.setJavaScriptCanOpenWindowsAutomatically(false);
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
      case "DL_RESEARCH": research(value); break;
      case "DL_SOURCE": Uri source=Uri.parse(value); if(!"https".equals(source.getScheme())||source.getHost()==null||source.getUserInfo()!=null){result.cancel();return true;} startActivity(new Intent(Intent.ACTION_VIEW,source)); break;
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
 private void research(String raw) throws Exception {
  JSONObject config=new JSONObject(raw); URI endpoint=new URI(config.getString("endpoint"));
  if(!"https".equals(endpoint.getScheme())||endpoint.getHost()==null||endpoint.getUserInfo()!=null||endpoint.getQuery()!=null||endpoint.getFragment()!=null||!(endpoint.getPath().isEmpty()||"/".equals(endpoint.getPath())))throw new IOException("URL inválida");
  String token=config.getString("token"); if(token.length()<24||token.length()>512||token.contains("\r")||token.contains("\n"))throw new IOException("Código inválido");
  byte[] payload=config.getJSONObject("query").toString().getBytes(StandardCharsets.UTF_8);
  new Thread(()->{
   JSONObject result; javax.net.ssl.HttpsURLConnection conn=null;
   try{
    conn=(javax.net.ssl.HttpsURLConnection)new URL(endpoint.toString().replaceAll("/$","")+"/research").openConnection();
    conn.setInstanceFollowRedirects(false); conn.setConnectTimeout(20000); conn.setReadTimeout(180000); conn.setRequestMethod("POST"); conn.setDoOutput(true);
    conn.setRequestProperty("Content-Type","application/json");conn.setRequestProperty("Authorization","Bearer "+token);
    try(OutputStream out=conn.getOutputStream()){out.write(payload);}
    int code=conn.getResponseCode();
    try(InputStream in=code>=200&&code<300?conn.getInputStream():conn.getErrorStream()){
     if(in==null)throw new IOException("HTTP "+code);
     ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>2000000)throw new IOException("Resposta muito extensa");bytes.write(buffer,0,n);}
     result=new JSONObject(bytes.toString("UTF-8"));
     if(code<200||code>=300){if(!result.has("error"))result.put("error","HTTP "+code);}
    }
   }catch(Exception e){result=new JSONObject();try{result.put("error","Não foi possível consultar o servidor. Confira a conexão, endereço e configuração.");}catch(JSONException ignored){}}
   finally{if(conn!=null)conn.disconnect();}
   final String json=result.toString();runOnUiThread(()->{if(!isDestroyed()&&web!=null&&HOME.equals(web.getUrl()))web.evaluateJavascript("window.autoResearchResult && window.autoResearchResult(JSON.parse("+JSONObject.quote(json)+"))",null);});
  }).start();
 }
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 @Override protected void onDestroy(){if(picker!=null){picker.onReceiveValue(null);picker=null;}if(web!=null)web.destroy();super.onDestroy();}
}
