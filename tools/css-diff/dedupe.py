import re,sys
def scan(s):
    """returns list of rules: dict(ctx, sel, sel_span, body_span, decls[(prop,imp,start,end)])"""
    rules=[];stack=[];i=0;n=len(s);start=0
    def skip_comment(i):
        j=s.find('*/',i+2); return n if j<0 else j+2
    seg_start=0
    while i<n:
        c=s[i]
        if s.startswith('/*',i): i=skip_comment(i); continue
        if c in '"\'':
            q=c;i+=1
            while i<n and s[i]!=q:
                if s[i]=='\\': i+=1
                i+=1
            i+=1; continue
        if c=='{':
            prelude=s[seg_start:i]
            pre=re.sub(r'/\*.*?\*/','',prelude,flags=re.S).strip()
            if pre.startswith('@'):
                stack.append(('at',re.sub(r'\s+',' ',pre)))
                i+=1; seg_start=i; continue
            # rule body: find matching }
            j=i+1;depth=1
            while j<n and depth:
                if s.startswith('/*',j): j=skip_comment(j); continue
                if s[j] in '"\'':
                    q=s[j];j+=1
                    while j<n and s[j]!=q:
                        if s[j]=='\\': j+=1
                        j+=1
                    j+=1; continue
                if s[j]=='{': depth+=1
                elif s[j]=='}': depth-=1
                j+=1
            body_s,body_e=i+1,j-1
            ctx=tuple(x[1] for x in stack)
            inkey=any(x.startswith('@keyframes') or x.startswith('@-webkit-keyframes') or x.startswith('@font-face') for x in ctx)
            sel=re.sub(r'\s+',' ',pre)
            sel=re.sub(r'\s*,\s*',',',sel)
            # leading whitespace of prelude
            lead=len(prelude)-len(prelude.lstrip())
            rules.append(dict(ctx=ctx,sel=sel,skip=inkey,sel_start=seg_start,body=(body_s,body_e),end=j))
            i=j; seg_start=i; continue
        if c=='}':
            if stack: stack.pop()
            i+=1; seg_start=i; continue
        if c==';' and not stack or (c==';'):
            # top-level @import etc
            i+=1; seg_start=i if not stack else seg_start
            if not stack: seg_start=i
            continue
        i+=1
    return rules
def decls(s,b,e):
    out=[];i=b;st=b;depth=0
    while i<=e:
        c=s[i] if i<e else ';'
        if i<e and s.startswith('/*',i):
            j=s.find('*/',i+2); i=j+2; continue
        if c in '"\'' and i<e:
            q=c;i+=1
            while i<e and s[i]!=q:
                if s[i]=='\\': i+=1
                i+=1
            i+=1; continue
        if c=='(': depth+=1
        elif c==')': depth-=1
        elif c==';' and depth==0:
            text=s[st:i]
            clean=re.sub(r'/\*.*?\*/','',text,flags=re.S).strip()
            if ':' in clean:
                prop=clean.split(':',1)[0].strip().lower()
                imp='!important' in clean.replace(' ','').lower()
                # span: from first non-space char of decl to i (include ;) if i<e
                k=st
                while k<i and s[k] in ' \t\r\n': k+=1
                # do not eat a leading comment: start at first char of property name
                m=re.search(re.escape(prop) if prop else r'\S',s[st:i],re.I)
                ds=st+(m.start() if m else 0)
                out.append((prop,imp,ds,i+1 if i<e else e))
            st=i+1
        i+=1
    return out
def dedupe(s):
    rules=scan(s)
    groups={}
    for idx,r in enumerate(rules):
        if r['skip']: continue
        r['decls']=decls(s,*r['body'])
        groups.setdefault((r['ctx'],r['sel']),[]).append(idx)
    remove=[]  # spans
    removed=0
    for key,idxs in groups.items():
        if len(idxs)<2: continue
        for a_pos,a in enumerate(idxs):
            later=[d for b in idxs[a_pos+1:] for d in rules[b]['decls']]
            for (p,imp,ds,de) in rules[a]['decls']:
                if p.startswith('--'):  # custom props: same rule — later wins too
                    pass
                if any(lp==p and (limp or not imp) for (lp,limp,_,_) in later):
                    remove.append((ds,de)); removed+=1
    # decide which rules become empty
    rm=set(remove)
    full=[]
    for r in rules:
        if r['skip'] or 'decls' not in r or not r['decls']: continue
        if all((ds,de) in rm for (_,_,ds,de) in r['decls']):
            rest=s[r['body'][0]:r['body'][1]]
            for (_,_,ds,de) in sorted(r['decls'],key=lambda x:-x[2]):
                rest=rest[:ds-r['body'][0]]+rest[de-r['body'][0]:]
            if not re.sub(r'/\*.*?\*/','',rest,flags=re.S).strip():
                # remove whole rule: from start of selector text (skip leading whitespace kept) to end
                ss=r['sel_start']
                while ss<len(s) and s[ss] in ' \t\r\n': ss+=1
                full.append((ss,r['end']))
                for (_,_,ds,de) in r['decls']: rm.discard((ds,de))
    spans=sorted(list(rm)+full,key=lambda x:-x[0])
    out=s
    for a,b in spans:
        out=out[:a]+out[b:]
    # tidy blank lines
    out=re.sub(r'\n[ \t]*\n[ \t]*\n+','\n\n',out)
    return out,removed,len(full)
if __name__=='__main__':
    src=open(sys.argv[1],encoding='utf-8').read()
    out,removed,full=dedupe(src)
    open(sys.argv[2],'w',encoding='utf-8').write(out)
    print('removed decls',removed,'removed rules',full, 'bytes',len(src),'->',len(out))
