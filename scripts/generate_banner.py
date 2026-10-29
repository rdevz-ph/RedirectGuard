import os
import shutil
import tempfile
import urllib.request
from PIL import Image, ImageDraw, ImageFont

# Automatically locate the repository root regardless of current working directory
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))
FONT_BASE_URL = "https://raw.githubusercontent.com/hprobotic/Google-Sans-Font/master/"

def fetch_font(font_name, temp_dir):
    dest = os.path.join(temp_dir, font_name)
    if not os.path.exists(dest):
        url = FONT_BASE_URL + font_name
        try:
            urllib.request.urlretrieve(url, dest)
        except Exception as e:
            print(f"Warning: could not download {font_name}: {e}")
            return None
    return dest

def get_font(font_name, size, temp_dir):
    font_path = fetch_font(font_name, temp_dir)
    if font_path and os.path.exists(font_path):
        try:
            return ImageFont.truetype(font_path, size)
        except Exception:
            pass
            
    # Fallback to system fonts
    for fallback in ["segoeuib.ttf" if "bold" in font_name.lower() else "segoeui.ttf", "arialbd.ttf" if "bold" in font_name.lower() else "arial.ttf"]:
        win_path = os.path.join("C:/Windows/Fonts", fallback)
        if os.path.exists(win_path):
            try:
                return ImageFont.truetype(win_path, size)
            except Exception:
                pass
                
    return ImageFont.load_default()

def wrap_text(text, font, max_width):
    words = text.split()
    lines = []
    current_line = []
    for word in words:
        test_line = " ".join(current_line + [word])
        if font.getlength(test_line) <= max_width:
            current_line.append(word)
        else:
            if current_line:
                lines.append(" ".join(current_line))
            current_line = [word]
    if current_line:
        lines.append(" ".join(current_line))
    return lines

def generate_social_preview(output_path=None):
    if output_path is None:
        output_path = os.path.join(PROJECT_ROOT, "docs", "social_preview.png")
    elif not os.path.isabs(output_path):
        output_path = os.path.join(PROJECT_ROOT, output_path)

    width, height = 1280, 640
    
    # Create temporary directory for fonts that auto-cleans up
    temp_dir = tempfile.mkdtemp(prefix="rg_font_")
    try:
        # Modern dark canvas (#0D1117)
        img = Image.new("RGBA", (width, height), (13, 17, 23, 255))
        draw = ImageDraw.Draw(img)
        
        # Load and place app logo
        icon_path = os.path.join(PROJECT_ROOT, "icon", "icon.png")
        logo_size = 360
        logo_x = 95
        logo_y = (height - logo_size) // 2
        
        if os.path.exists(icon_path):
            icon = Image.open(icon_path).convert("RGBA")
            icon = icon.resize((logo_size, logo_size), Image.Resampling.LANCZOS)
            img.paste(icon, (logo_x, logo_y), icon)
        else:
            print(f"Error: App icon not found at {icon_path}")
            
        # Download and load Google Sans into memory
        font_title = get_font("GoogleSans-Bold.ttf", 78, temp_dir)
        font_desc = get_font("GoogleSans-Regular.ttf", 31, temp_dir)
        
        text_x = 515
        max_text_w = width - text_x - 95
        
        title = "Redirect Guard"
        description = "An open-source Android tool that helps prevent unwanted ad redirects and deep links to external apps, with permission prompts and automatic return to the previous app."
        
        desc_lines = wrap_text(description, font_desc, max_text_w)
        
        t_bbox = font_title.getbbox(title)
        title_h = t_bbox[3] - t_bbox[1]
        
        line_h = 50
        spacing_title_desc = 54
        total_text_h = title_h + spacing_title_desc + len(desc_lines) * line_h
        
        start_y = (height - total_text_h) // 2
        
        # Draw Title
        draw.text((text_x, start_y), title, fill=(255, 255, 255, 255), font=font_title)
        
        # Draw Description with generous spacing
        curr_y = start_y + title_h + spacing_title_desc
        for line in desc_lines:
            draw.text((text_x, curr_y), line, fill=(203, 213, 225, 255), font=font_desc)
            curr_y += line_h
            
        os.makedirs(os.path.dirname(output_path), exist_ok=True)
        img.save(output_path, "PNG")
        print(f"Generated social preview at {output_path}")
    finally:
        # Immediately clean up all temporary font files
        shutil.rmtree(temp_dir, ignore_errors=True)
        print("Cleaned up temporary font files.")

if __name__ == "__main__":
    generate_social_preview()
