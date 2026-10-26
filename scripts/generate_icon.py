import os
import math
from PIL import Image, ImageDraw

def create_shield_polygon(cx, cy, width, height, steps=60):
    """
    Returns points for a sleek, beautifully proportioned shield.
    """
    hw = width / 2.0
    hh = height / 2.0
    top = cy - hh
    shoulder_y = top + height * 0.07
    mid_y = cy + height * 0.12
    bot = cy + hh
    
    points = []
    
    # 1. Top edge: gentle dip from left shoulder to center crest
    for i in range(steps // 2):
        t = i / float(steps // 2 - 1)
        x = (cx - hw) + t * (2 * hw)
        crest_offset = 24.0 * (1.0 - 4.0 * (t - 0.5)**2)
        y = shoulder_y - crest_offset
        points.append((x, y))
        
    # 2. Right vertical-ish edge down to mid_y
    for i in range(1, steps // 4):
        t = i / float(steps // 4)
        x = cx + hw
        y = shoulder_y + t * (mid_y - shoulder_y)
        points.append((x, y))
        
    # 3. Right curve from mid_y to bottom point (cubic bezier)
    p0 = (cx + hw, mid_y)
    p1 = (cx + hw, mid_y + (bot - mid_y) * 0.45)
    p2 = (cx + hw * 0.35, bot - (bot - mid_y) * 0.08)
    p3 = (cx, bot)
    for i in range(1, steps):
        t = i / float(steps)
        inv = 1.0 - t
        x = inv**3 * p0[0] + 3 * inv**2 * t * p1[0] + 3 * inv * t**2 * p2[0] + t**3 * p3[0]
        y = inv**3 * p0[1] + 3 * inv**2 * t * p1[1] + 3 * inv * t**2 * p2[1] + t**3 * p3[1]
        points.append((x, y))
        
    # 4. Left curve from bottom point back to (cx - hw, mid_y)
    right_curve = points[-steps+1:]
    for pt in reversed(right_curve):
        points.append((cx - (pt[0] - cx), pt[1]))
        
    # 5. Left edge up to shoulder
    for i in range(1, steps // 4):
        t = i / float(steps // 4)
        x = cx - hw
        y = mid_y - t * (mid_y - shoulder_y)
        points.append((x, y))
        
    return points

def eval_cubic_bezier(p0, p1, p2, p3, steps=25):
    pts = []
    for i in range(steps + 1):
        t = i / float(steps)
        inv = 1.0 - t
        x = inv**3 * p0[0] + 3 * inv**2 * t * p1[0] + 3 * inv * t**2 * p2[0] + t**3 * p3[0]
        y = inv**3 * p0[1] + 3 * inv**2 * t * p1[1] + 3 * inv * t**2 * p2[1] + t**3 * p3[1]
        pts.append((x, y))
    return pts

def create_redirect_arrow_polygon(cx, cy, width, height):
    """
    Generates points for a classic Material Design redirect / forward arrow:
    Based on M14,9 V5 L21,12 L14,19 V14.9 C9,14.9 5.5,16.5 3,20 C4,15 7,10 14,9 Z
    Points forward to the right (the natural redirect direction).
    Centered at (cx, cy).
    """
    sx = width / 18.0
    sy = height / 15.0
    
    def transform(x, y):
        return (cx + (x - 12.5) * sx, cy + (y - 12.5) * sy)
    
    points = []
    
    # Arrow head top: (14, 5) -> tip: (21, 12) -> bottom: (14, 19)
    points.append(transform(14, 5))
    points.append(transform(21, 12))
    points.append(transform(14, 19))
    points.append(transform(14, 14.9))
    
    # Bottom curve: (14, 14.9) -> (3, 20)
    p0 = transform(14, 14.9)
    p1 = transform(9, 14.9)
    p2 = transform(5.5, 16.5)
    p3 = transform(3, 20)
    bottom_curve = eval_cubic_bezier(p0, p1, p2, p3, steps=25)
    points.extend(bottom_curve[1:])
    
    # Top curve: (3, 20) -> (14, 9)
    q0 = transform(3, 20)
    q1 = transform(4, 15)
    q2 = transform(7, 10)
    q3 = transform(14, 9)
    top_curve = eval_cubic_bezier(q0, q1, q2, q3, steps=25)
    points.extend(top_curve[1:])
    
    return points

def render_icon(size=1024, mode="standard"):
    """
    mode: 'standard' (rounded squircle), 'round' (circle), 'foreground' (transparent bg)
    """
    scale = 2 # 2x supersampling for ultra-crisp edges
    canvas_w = size * scale
    canvas_h = size * scale
    cx = canvas_w / 2.0
    cy = canvas_h / 2.0
    s = scale * (size / 1024.0)
    
    img = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Modern clean palette
    bg_dark = (15, 23, 42, 255)       # Slate-900: #0F172A
    border_slate = (30, 41, 59, 255)  # Slate-800: #1E293B
    
    if mode == "standard":
        padding = 48 * s
        radius = 215 * s
        bbox = [padding, padding, canvas_w - padding, canvas_h - padding]
        draw.rounded_rectangle(bbox, radius=int(radius), fill=bg_dark, outline=border_slate, width=int(8 * s))
    elif mode == "round":
        padding = 45 * s
        bbox = [padding, padding, canvas_w - padding, canvas_h - padding]
        draw.ellipse(bbox, fill=bg_dark, outline=border_slate, width=int(8 * s))
    # 'foreground' keeps transparent background
    
    # Shield dimensions
    shield_w = 480 * s
    shield_h = 580 * s
    shield_cy = cy + 10 * s
    
    shield_pts = create_shield_polygon(cx, shield_cy, shield_w, shield_h)
    
    # Mask for shield vertical gradient
    shield_mask = Image.new("L", (canvas_w, canvas_h), 0)
    mask_draw = ImageDraw.Draw(shield_mask)
    mask_draw.polygon(shield_pts, fill=255)
    
    # Shield vertical gradient: Vibrant Sky Blue (#0284C7) to Deep Ocean (#0369A1)
    shield_top_col = (2, 132, 199)
    shield_bot_col = (3, 105, 161)
    
    grad = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    grad_draw = ImageDraw.Draw(grad)
    min_y = int(min(pt[1] for pt in shield_pts))
    max_y = int(max(pt[1] for pt in shield_pts))
    span_y = max(1, max_y - min_y)
    
    for y in range(min_y, max_y + 1):
        ratio = (y - min_y) / float(span_y)
        r = int(shield_top_col[0] + ratio * (shield_bot_col[0] - shield_top_col[0]))
        g = int(shield_top_col[1] + ratio * (shield_bot_col[1] - shield_top_col[1]))
        b = int(shield_top_col[2] + ratio * (shield_bot_col[2] - shield_top_col[2]))
        grad_draw.line([(0, y), (canvas_w, y)], fill=(r, g, b, 255))
        
    img.paste(grad, (0, 0), shield_mask)
    
    # Crisp inner shield border
    shield_border_col = (56, 189, 248, 255) # Sky-400: #38BDF8
    draw.line(shield_pts + [shield_pts[0]], fill=shield_border_col, width=int(10 * s))
    
    # Draw Redirect Forward Arrow
    arrow_w = 260 * s
    arrow_h = 220 * s
    arrow_cy = shield_cy - 10 * s
    arrow_pts = create_redirect_arrow_polygon(cx, arrow_cy, arrow_w, arrow_h)
    
    white = (255, 255, 255, 255)
    draw.polygon(arrow_pts, fill=white)
    
    # Downscale with high quality Lanczos resampling
    final = img.resize((size, size), Image.Resampling.LANCZOS)
    return final

def main():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    icon_dir = os.path.join(base_dir, "icon")
    res_dir = os.path.join(base_dir, "app", "src", "main", "res")
    
    os.makedirs(icon_dir, exist_ok=True)
    
    print("Generating master icons in 'icon/' folder...")
    icon_std = render_icon(512, mode="standard")
    icon_std.save(os.path.join(icon_dir, "icon.png"), "PNG")
    
    icon_rnd = render_icon(512, mode="round")
    icon_rnd.save(os.path.join(icon_dir, "icon_round.png"), "PNG")
    
    icon_fg = render_icon(512, mode="foreground")
    icon_fg.save(os.path.join(icon_dir, "icon_foreground.png"), "PNG")
    
    icon_bg = Image.new("RGBA", (512, 512), (15, 23, 42, 255))
    icon_bg.save(os.path.join(icon_dir, "icon_background.png"), "PNG")
    
    print("Master icons generated in 'icon/'.")
    
    # Android mipmap densities
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }
    
    print("Exporting Android mipmaps...")
    for folder, dim in densities.items():
        out_folder = os.path.join(res_dir, folder)
        os.makedirs(out_folder, exist_ok=True)
        
        # Save standard & round pngs
        icon_std.resize((dim, dim), Image.Resampling.LANCZOS).save(os.path.join(out_folder, "ic_launcher.png"), "PNG")
        icon_rnd.resize((dim, dim), Image.Resampling.LANCZOS).save(os.path.join(out_folder, "ic_launcher_round.png"), "PNG")
        
        # Remove old template .webp files if present
        for old_file in ["ic_launcher.webp", "ic_launcher_round.webp"]:
            p = os.path.join(out_folder, old_file)
            if os.path.exists(p):
                os.remove(p)
                
        print(f"  - {folder}: {dim}x{dim}")
        
    print("All icons successfully generated and placed!")

if __name__ == "__main__":
    main()
