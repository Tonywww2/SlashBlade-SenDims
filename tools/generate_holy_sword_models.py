"""Build closed, opaque OBJ swords for SlashBlade's world and inventory groups.

References: Good Smile Saber Lily Golden Caliburn; BANDAI PROPLICA Excalibur.
All details are original geometry, not extracted meshes or copied image textures.
"""
from __future__ import annotations
import collections
import json
import math
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'src/main/resources/assets/slashblade_sendims/model/holy_swords'
# The blue grip spans X=18..77. Native hardpointA holds the model at X=0.
GRIP_CENTER_X = (18.0 + 77.0) / 2.0
PALETTE = [
    (226, 234, 242), (174, 192, 209), (250, 252, 255), (242, 191, 73),
    (179, 119, 31), (255, 220, 118), (24, 48, 111), (43, 84, 157),
    (111, 27, 46), (173, 48, 67), (240, 230, 199), (205, 217, 223),
    (99, 117, 133), (221, 64, 69), (106, 170, 219), (27, 36, 55),
]
SILVER, STEEL, WHITE, GOLD, DARK_GOLD, PALE_GOLD, BLUE, MID_BLUE, RED, LIGHT_RED, IVORY, LIGHT_STEEL, ENGRAVE, RUBY, SKY, DARK = range(16)

class Mesh:
    def __init__(self): self.vertices, self.faces, self.materials = [], [], []
    def vertex(self, p): self.vertices.append(tuple(float(v) for v in p)); return len(self.vertices)-1
    def face(self, ids, material):
        for i in range(1, len(ids)-1):
            self.faces.append((ids[0], ids[i], ids[i+1])); self.materials.append(material)
    def add(self, other, transform=lambda p:p):
        offset = len(self.vertices)
        self.vertices += [tuple(transform(np.array(p, dtype=float))) for p in other.vertices]
        self.faces += [tuple(i+offset for i in f) for f in other.faces]
        self.materials += other.materials
    def validate(self):
        edges = collections.Counter()
        directed = collections.Counter()
        for f in self.faces:
            a,b,c = [np.array(self.vertices[i]) for i in f]
            assert np.linalg.norm(np.cross(b-a,c-a)) > 1e-7, ('Degenerate face',f)
            for u,v in zip(f, (f[1],f[2],f[0])):
                edges[tuple(sorted((u,v)))] += 1; directed[(u,v)] += 1
        assert all(n==2 for n in edges.values()), 'Open/non-manifold edge'
        assert all(directed[(v,u)]==n for (u,v),n in directed.items()), 'Inconsistent winding'

def cross2(a,b,c): return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0])
def triangulate(poly):
    ids=list(range(len(poly))); result=[]
    while len(ids)>3:
        for k in range(len(ids)):
            a,b,c=ids[k-1],ids[k],ids[(k+1)%len(ids)]
            if cross2(poly[a],poly[b],poly[c]) <= 1e-9: continue
            inside=lambda p: all(v>=-1e-8 for v in (cross2(poly[a],poly[b],p),cross2(poly[b],poly[c],p),cross2(poly[c],poly[a],p)))
            if any(inside(poly[j]) for j in ids if j not in (a,b,c)): continue
            result.append((a,b,c)); ids.pop(k); break
        else: raise ValueError('Invalid polygon')
    result.append(tuple(ids)); return result

def prism(poly, low, high, material):
    poly=list(poly)
    if sum(a[0]*b[1]-b[0]*a[1] for a,b in zip(poly,poly[1:]+poly[:1]))<0: poly.reverse()
    mesh=Mesh(); n=len(poly)
    for z in (low,high):
        for x,y in poly: mesh.vertex((x,y,z))
    for a,b,c in triangulate(poly):
        mesh.face((c,b,a),material); mesh.face((a+n,b+n,c+n),material)
    for i in range(n):
        j=(i+1)%n; mesh.face((i,j,j+n,i+n),material)
    mesh.validate(); return mesh

def panel(mesh, poly, depth, material):
    mesh.add(prism(poly,depth,depth+0.5,material))
    mesh.add(prism(poly,-depth-0.5,-depth,material))

def line(mesh, a, b, width, depth, material=GOLD):
    a,b=np.array(a,float),np.array(b,float); delta=b-a
    side=np.array((-delta[1],delta[0]))/np.linalg.norm(delta)*width/2
    panel(mesh,[a-side,b-side,b+side,a+side],depth,material)

def loft(rings, material, sides=8, bevel=False):
    mesh=Mesh()
    for x,width,thick in rings:
        for i in range(sides):
            angle=(i/sides)*math.tau
            mesh.vertex((x,math.cos(angle)*width,math.sin(angle)*thick))
    # Ring angles are CCW when viewed along +X; outward faces are oriented below.
    center=np.mean(mesh.vertices,axis=0)
    def outward(ids,mat):
        pts=np.array([mesh.vertices[i] for i in ids]); normal=np.cross(pts[1]-pts[0],pts[2]-pts[0])
        if np.dot(normal,np.mean(pts,axis=0)-center)<0: ids=tuple(reversed(ids))
        mesh.face(ids,mat)
    outward(tuple(range(sides)),material)
    for k in range(len(rings)-1):
        for i in range(sides):
            j=(i+1)%sides
            mat = (WHITE if i%2==0 else STEEL) if bevel else material
            outward((k*sides+i,k*sides+j,(k+1)*sides+j,(k+1)*sides+i),mat)
    outward(tuple((len(rings)-1)*sides+i for i in range(sides)),material)
    mesh.validate(); return mesh

def gem(mesh,x,y,radius,depth):
    shape=[(x-radius,y),(x,y-radius),(x+radius,y),(x,y+radius)]
    panel(mesh,shape,depth,GOLD)
    shape=[(x-radius*.65,y),(x,y-radius*.65),(x+radius*.65,y),(x,y+radius*.65)]
    panel(mesh,shape,depth+0.65,RUBY)

def sword(name, broken=False):
    mesh=Mesh(); cal=name=='caliburn'
    length=266 if cal else 280
    if broken:
        body=[(-12,12,2.7),(-72,11,2.2),(-85,2,0.3)]
    else:
        body=[(-12,12,2.7),(-80,11.5,2.6),(-length+34,9.2,2.2),(-length,0.16,0.13)]
    mesh.add(loft(body,SILVER,4,True))
    # Golden framing and blue heraldic ricasso, present on both sides.
    panel(mesh,[(-8,-12),(-8,12),(-52,11),(-67,0),(-52,-11)],2.9,GOLD)
    panel(mesh,[(-13,-9),(-13,9),(-49,8),(-59,0),(-49,-8)],3.5,BLUE)
    for side in (-1,1):
        line(mesh,(-18,side*3),(-43,side*6),1.2,4.1)
        line(mesh,(-43,side*6),(-52,0),1.2,4.1)
    panel(mesh,[(-19,-1.3),(-19,1.3),(-45,1.3),(-50,0),(-45,-1.3)],4.2,PALE_GOLD)
    if cal:
        # More ornate king-selection sword: gold flourishes and red central jewel.
        for side in (-1,1):
            line(mesh,(-28,side*5),(-39,side*1.5),1.2,4.3)
            line(mesh,(-40,side*7),(-47,side*2),1.0,4.3)
        gem(mesh,-24,0,3.0,4.8)
    # Engraved marks stop before the broken tip in the damaged group.
    for i in range(7 if not broken else 1):
        x=-78-i*8
        line(mesh,(x,-2),(x-3,2),0.7,2.7,ENGRAVE)
        line(mesh,(x-3,2),(x-5,-1),0.7,2.7,ENGRAVE)
    # Separate closed crossguard wings avoid concave fan caps and reversed faces.
    guard=Mesh()
    guard.add(prism([(-4,-12),(10,-12),(15,0),(10,12),(-4,12),(-9,0)],-5.4,5.4,GOLD))
    wing=[(-3,9),(5,11),(12,23),(18,43),(10,46),(4,28)] if cal else [(-4,10),(7,11),(15,31),(22,46),(12,51),(3,30)]
    # Reflect the wings about the guard's X=3 center so they sweep toward the
    # blade (-X). Rebuild each prism to retain outward winding on both sides.
    def guard_point(x,y,side): return (6-x,y*side)
    for side in (-1,1):
        guard.add(prism([guard_point(x,y,side) for x,y in wing],-4.8,4.8,GOLD))
        inset=[(3,15),(7,16),(15,39),(12,41),(6,26)] if cal else [(3,17),(6,18),(18,43),(15,45),(6,29)]
        panel(guard,[guard_point(x,y,side) for x,y in inset],5.0,BLUE)
        line(guard,guard_point(8,26,side),guard_point(14,41,side),0.8,5.7,PALE_GOLD)
    # Thicken the entire guard, keeping its surface ornaments aligned.
    mesh.add(guard,lambda p:(p[0],p[1],p[2]*1.25))
    mesh.add(loft([(10,6.0,4.0),(18,6.0,4.0)],DARK_GOLD))
    mesh.add(loft([(18,5.0,4.0),(77,4.5,3.6)],BLUE))
    for x in (19,28,38,48,58,68,77):
        mesh.add(loft([(x,5.1,4.1),(x+1.6,5.1,4.1)],DARK_GOLD if cal else MID_BLUE))
    mesh.add(loft([(78,6,4.5),(83,6,4.5)],GOLD))
    if cal:
        mesh.add(loft([(83,5,4),(90,9,5),(97,7,4),(102,1,1)],GOLD))
        gem(mesh,90,0,3.7,5.4)
    else:
        mesh.add(loft([(83,6,4.5),(89,6,4.5)],GOLD))
        mesh.add(loft([(89,4.8,3.5),(91,4.8,3.5)],DARK_GOLD))
    mesh.validate(); return mesh

def sheath(name):
    mesh=Mesh(); cal=name=='caliburn'; length=274 if cal else 288
    mesh.add(loft([(-5,16,5.5),(-length+25,13,4.8),(-length,3,2)],RED if cal else IVORY))
    mesh.add(loft([(0,19,6.5),(-12,19,6.5)],GOLD))
    mesh.add(loft([(-13,17,6),(-67,16.5,5.9)],BLUE))
    for x in (-16,-67,-length+30):
        mesh.add(loft([(x,17 if x>-70 else 14.6,6.1),(x-3,17 if x>-70 else 14.6,6.1)],GOLD))
    mesh.add(loft([(-length+30,14.5,5.5),(-length+7,8,4),(-length-3,1,1)],GOLD))
    for a,b in [((-18,-13),(-63,13)),((-18,13),(-63,-13))]: line(mesh,a,b,1.6,6.5,GOLD)
    line(mesh,(-14,0),(-64,0),1.2,7.1,PALE_GOLD)
    gem(mesh,-40,0,4.8,7.8)
    # Distinct sheath ornamentation: burgundy selection sword / ivory-blue Avalon-inspired scabbard.
    if cal:
        for x in (-100,-140,-180,-220):
            panel(mesh,[(x-2,-1),(x,-3),(x+2,-1),(x,1)],5.7,PALE_GOLD)
    else:
        for side in (-1,1):
            line(mesh,(-70,side*10),(-245,side*7),1.8,5.7,GOLD)
            line(mesh,(-77,side*7),(-218,side*4),2.5,6.2,BLUE)
        for x in (-100,-150,-200):
            panel(mesh,[(x-8,0),(x,-4),(x+8,0),(x,4)],6.5,GOLD)
            panel(mesh,[(x-5,0),(x,-2),(x+5,0),(x,2)],7.1,BLUE)
    mesh.validate(); return mesh

def texture(path):
    image=Image.new('RGBA',(128,128),(255,255,255,255)); px=image.load()
    for i,color in enumerate(PALETTE):
        for y in range(32):
            for x in range(32):
                factor=0.96+0.08*(x/31)
                px[(i%4)*32+x,(i//4)*32+y]=tuple(min(255,round(c*factor)) for c in color)+(255,)
    image.save(path)

def inventory_icon(mesh,center_x,scabbard=None):
    result=Mesh(); enlargement=1.75; scale=0.26*enlargement
    # The blade points along -X in the world mesh. A +90 degree Z rotation
    # puts the tip at -Y in the inventory, with the handle at the top.
    result.add(mesh,lambda p:np.array((-p[1],p[0]-center_x,p[2]))*scale)
    if scabbard is not None:
        sheath_center=(min(p[0] for p in scabbard.vertices)+max(p[0] for p in scabbard.vertices))/2
        # Keep the sheath horizontal, below center, and completely behind
        # the blade even where its raised ornaments cross the sword.
        result.add(scabbard,lambda p:np.array((p[0]-sheath_center,p[1],p[2]))*scale+np.array((0,-20,-6))*enlargement)
    # The native GUI durability frame reaches Z=5.06. Move the entire icon
    # forward so even the rear of the sheath stays in front of the frame.
    result.vertices=[(x,y,z+24.0) for x,y,z in result.vertices]
    return result

def write_obj(path,groups):
    lines=['# Fate holy swords. Closed opaque solids; all faces are triangles.']
    vertex_offset=0; normal_offset=0
    for group,mesh in groups.items():
        mesh.validate()
        lines.append('g '+group)
        for p in mesh.vertices: lines.append('v '+' '.join(f'{v:.6f}' for v in p))
        for material in mesh.materials:
            u=(material%4+.5)/4; v=1-(material//4+.5)/4
            lines.append(f'vt {u:.6f} {v:.6f}')
        for face in mesh.faces:
            a,b,c=[np.array(mesh.vertices[i]) for i in face]
            n=np.cross(b-a,c-a); n/=np.linalg.norm(n)
            lines.append('vn '+' '.join(f'{v:.6f}' for v in n))
        for k,face in enumerate(mesh.faces):
            index=normal_offset+k+1
            lines.append('f '+' '.join(f'{v+vertex_offset+1}/{index}/{index}' for v in face))
        vertex_offset+=len(mesh.vertices); normal_offset+=len(mesh.faces)
    path.write_text('\n'.join(lines)+'\n',encoding='utf-8')

def render_panel(image,mesh,box,reverse=False,edge=False,inventory=False):
    # Use a depth buffer: long beveled faces overlap small ornaments in projection.
    x0,y0,x1,y1=box
    camera=np.array((0.12,0.20,1.0 if not reverse else -1.0))
    if edge: camera=np.array((0.1,1.0,0.15))
    if inventory: camera=np.array((0.,0.,1.))
    camera/=np.linalg.norm(camera)
    up=np.array((0.,1.,0.) if inventory else (-1.,0.,0.)); right=np.cross(up,camera); right/=np.linalg.norm(right); up=np.cross(camera,right)
    pts=np.array(mesh.vertices); proj=np.column_stack((pts@right,pts@up)); depths=pts@camera
    minimum=proj.min(axis=0); maximum=proj.max(axis=0)
    scale=min((x1-x0)/(maximum[0]-minimum[0]),(y1-y0)/(maximum[1]-minimum[1]))*.94
    mid=(minimum+maximum)/2
    if inventory:
        # SlashBladeTEISR uses an identity GUI transform and a 0.008 scale,
        # so an inventory slot spans 125 OBJ units in each screen axis.
        scale=min(x1-x0,y1-y0)/125; mid=np.zeros(2)
    screen=(proj-mid)*np.array((scale,-scale))+np.array(((x0+x1)/2,(y0+y1)/2))
    pixels=np.array(image)
    zbuffer=np.full((image.height,image.width),-np.inf)
    light=np.array((-.3,.25,1 if not reverse else -1)); light/=np.linalg.norm(light)
    for k in range(len(mesh.faces)):
        f=mesh.faces[k]; p=pts[list(f)]; n=np.cross(p[1]-p[0],p[2]-p[0]); n/=np.linalg.norm(n)
        if n@camera<=0: continue
        shade=.61+.39*max(0,n@light)
        color=tuple(round(v*shade) for v in PALETTE[mesh.materials[k]])
        triangle=screen[list(f)]
        left=max(x0,int(np.floor(triangle[:,0].min())))
        right_edge=min(x1-1,int(np.ceil(triangle[:,0].max())))
        top=max(y0,int(np.floor(triangle[:,1].min())))
        bottom=min(y1-1,int(np.ceil(triangle[:,1].max())))
        if left>right_edge or top>bottom: continue
        xx,yy=np.meshgrid(np.arange(left,right_edge+1)+.5,np.arange(top,bottom+1)+.5)
        a,b,c=triangle
        denom=cross2(a,b,c)
        if abs(denom)<1e-10: continue
        u=((b[0]-xx)*(c[1]-yy)-(b[1]-yy)*(c[0]-xx))/denom
        v=((c[0]-xx)*(a[1]-yy)-(c[1]-yy)*(a[0]-xx))/denom
        w=1-u-v
        depth=u*depths[f[0]]+v*depths[f[1]]+w*depths[f[2]]
        region=zbuffer[top:bottom+1,left:right_edge+1]
        mask=(u>=-1e-8)&(v>=-1e-8)&(w>=-1e-8)&(depth>region)
        region[mask]=depth[mask]
        pixels[top:bottom+1,left:right_edge+1][mask]=color
    image.paste(Image.fromarray(pixels))

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    metrics={}; meshes={}; icons={}
    for name in ('caliburn','excalibur'):
        blade=sword(name); broken=sword(name,True); scabbard=sheath(name)
        center_x=(min(p[0] for p in blade.vertices)+max(p[0] for p in blade.vertices))/2
        groups={'blade':blade,'sheath':scabbard,'blade_damaged':broken,
                'blade_fragment':loft([(-90,11,2),(-155,8,1.8),(-170,.2,.1)],SILVER,4,True),
                'item_blade':inventory_icon(blade,center_x,scabbard),
                'item_bladens':inventory_icon(blade,center_x),
                'item_damaged':inventory_icon(broken,center_x)}
        # Move all world parts together so the grip meets the hand, including
        # the damaged blade and the sheath used by the native sheathing motion.
        for part in ('blade','sheath','blade_damaged','blade_fragment'):
            shifted=Mesh()
            shifted.add(groups[part],lambda p:(p[0]-GRIP_CENTER_X,p[1],p[2]))
            groups[part]=shifted
        write_obj(OUT/(name+'.obj'),groups); texture(OUT/(name+'.png'))
        metrics[name]={k:{'vertices':len(v.vertices),'triangles':len(v.faces),'closed':True} for k,v in groups.items()}
        meshes[name]=(blade,scabbard)
        icons[name]=groups['item_blade']
    # Native potion icon; fully opaque pixels avoid model transparency issues.
    effect=ROOT/'src/main/resources/assets/slashblade_sendims/textures/mob_effect/mana_burst.png'
    effect.parent.mkdir(parents=True,exist_ok=True)
    image=Image.new('RGBA',(18,18),(25,38,65,255)); draw=ImageDraw.Draw(image)
    draw.polygon([(9,1),(11,6),(16,9),(11,11),(9,17),(7,11),(2,9),(7,6)],fill=(255,208,85,255))
    draw.polygon([(9,4),(10,8),(13,9),(10,10),(9,14),(8,10),(5,9),(8,8)],fill=(255,252,224,255)); image.save(effect)
    # The light renderer uses vertex colors, but retain a valid texture resource for its API.
    light=ROOT/'src/main/resources/assets/slashblade_sendims/textures/effect/holy_sword_light.png'
    light.parent.mkdir(parents=True,exist_ok=True); Image.new('RGBA',(2,2),(255,255,255,255)).save(light)
    preview=Image.new('RGB',(1800,1200),(17,24,39)); draw=ImageDraw.Draw(preview)
    font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',26)
    small=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',18)
    draw.text((60,24),'FATE / HOLY SWORDS',font=font,fill=(246,214,143))
    for col,name in enumerate(('caliburn','excalibur')):
        base=col*900
        draw.text((base+65,80),name.upper(),font=font,fill=(240,239,226))
        blade,scabbard=meshes[name]
        for j,(mesh,rev,edge,label) in enumerate(((blade,False,False,'BLADE / FRONT'),(blade,True,False,'BLADE / BACK'),(scabbard,False,False,'SCABBARD'),(blade,False,True,'EDGE / THICKNESS'))):
            left=base+35+j*215
            render_panel(preview,mesh,(left,160,left+195,1120),rev,edge)
            draw.text((left,1160),label,font=small,fill=(155,171,196))
    preview.save(ROOT/'holy-swords-preview.png')
    inventory_preview=Image.new('RGB',(800,500),(17,24,39)); draw=ImageDraw.Draw(inventory_preview)
    for col,name in enumerate(('caliburn','excalibur')):
        base=col*400
        draw.text((base+48,32),name.upper(),font=font,fill=(246,214,143))
        box=(base+48,90,base+352,394)
        draw.rectangle(box,fill=(25,34,49),outline=(68,81,103),width=2)
        render_panel(inventory_preview,icons[name],box,inventory=True)
        draw.text((base+48,430),'INVENTORY / TIP DOWN',font=small,fill=(155,171,196))
    inventory_preview.save(ROOT/'holy-swords-inventory-preview.png')
    (ROOT/'model-validation.json').write_text(json.dumps(metrics,indent=2),encoding='utf-8')
    print(json.dumps(metrics,indent=2))

if __name__=='__main__': main()
