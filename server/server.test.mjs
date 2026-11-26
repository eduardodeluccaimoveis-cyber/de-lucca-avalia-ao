import test from 'node:test';
import assert from 'node:assert/strict';
import {validate,candidates,createServer} from './server.mjs';
const q=validate({type:'Apartamento',basis:'Privativa',purpose:'venda',area:72,city:'Ribeirão Preto/SP',district:'Ribeirânia'});
test('validação de dados',()=>{assert.throws(()=>validate({...q,area:0}));assert.throws(()=>validate({...q,basis:'Inexistente'}));});
test('aceita somente fontes consultadas e amostras coerentes',()=>{
 const s={name:'Anúncio de teste',url:'https://example.com/imovel/1',area:70,price:500000,location:'Ribeirânia',evidence:'70 m², R$ 500.000',type:q.type,basis:q.basis,purpose:q.purpose};
 const r={output:[{type:'web_search_call',status:'completed',action:{sources:[{url:s.url}]}},{content:[{type:'output_text',text:JSON.stringify({samples:[s,s,{...s,url:'https://example.com/imovel/2'},{...s,price:0}],notes:''})}]}]};
 assert.equal(candidates(r,q).samples.length,1);assert.throws(()=>candidates({output:[]},q));
});
test('API exige autenticação e valida requisição',async()=>{
 const secret='teste-1234567890-1234567890';const server=createServer({secret,run:async()=>({samples:[],notes:'Teste'})});await new Promise(r=>server.listen(0,'127.0.0.1',r));
 try{const url='http://127.0.0.1:'+server.address().port+'/research';const req=(token,data)=>fetch(url,{method:'POST',headers:{Authorization:'Bearer '+token,'Content-Type':'application/json'},body:JSON.stringify(data)});assert.equal((await req('errado',q)).status,401);assert.equal((await req(secret,{...q,area:0})).status,400);assert.equal((await req(secret,q)).status,200);}finally{server.close();}
});
