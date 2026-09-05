import math
from PIL import Image, ImageDraw, ImageFilter

def create_base_icon(size=1024, is_round=False):
    # Render at high resolution
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    
    # 1. Background Gradient Mesh
    bg = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            # Gradient from top-left (#0F172A) to center (#1E1B4B) to bottom-right (#3B0764)
            t1 = (x + y) / (2 * size)
            r = int(15 * (1 - t1) + 30 * (1 - abs(t1 - 0.5) * 2) + 55 * t1)
            g = int(23 * (1 - t1) + 27 * (1 - abs(t1 - 0.5) * 2) + 10 * t1)
            b = int(42 * (1 - t1) + 75 * (1 - abs(t1 - 0.5) * 2) + 95 * t1)
            bg.putpixel((x, y), (r, g, b, 255))
            
    # Apply mask for round if needed, or rounded squircle
    mask = Image.new("L", (size, size), 0)
    mask_draw = ImageDraw.Draw(mask)
    if is_round:
        mask_draw.ellipse([0, 0, size, size], fill=255)
    else:
        # squircle radius ~ 22% of size
        rad = int(size * 0.22)
        mask_draw.rounded_rectangle([0, 0, size, size], radius=rad, fill=255)
    
    img.paste(bg, (0, 0), mask)
    
    # Draw glow layers on separate layer
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(glow)
    
    # Central warm amber glow
    cx, cy = size // 2, int(size * 0.52)
    g_draw.ellipse([cx - int(size*0.35), cy - int(size*0.32), cx + int(size*0.35), cy + int(size*0.32)], fill=(245, 158, 11, 45))
    glow = glow.filter(ImageFilter.GaussianBlur(int(size * 0.08)))
    img = Image.alpha_composite(img, glow)
    
    # Foreground Draw
    fg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(fg)
    
    # Decorative Dotted Celestial Ring
    ring_r = int(size * 0.36)
    for angle in range(0, 360, 8):
        rad_a = math.radians(angle)
        rx = cx + int(ring_r * math.cos(rad_a))
        ry = cy + int(ring_r * math.sin(rad_a))
        dot_r = 2 if angle % 24 != 0 else 4
        col = (56, 189, 248, 140) if angle % 16 == 0 else (253, 230, 138, 90)
        draw.ellipse([rx - dot_r, ry - dot_r, rx + dot_r, ry + dot_r], fill=col)

    # Magic Star Spark at top
    star_x = cx
    star_y = int(size * 0.28)
    star_glow_r = int(size * 0.12)
    
    # Star outer halo
    s_glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    sg_draw = ImageDraw.Draw(s_glow)
    sg_draw.ellipse([star_x - star_glow_r, star_y - star_glow_r, star_x + star_glow_r, star_y + star_glow_r], fill=(251, 191, 36, 110))
    s_glow = s_glow.filter(ImageFilter.GaussianBlur(int(size * 0.04)))
    img = Image.alpha_composite(img, s_glow)
    
    # 4-point Diamond Star
    def draw_star(center_x, center_y, ray_len, width, fill_col):
        pts = [
            (center_x, center_y - ray_len),
            (center_x + width, center_y),
            (center_x, center_y + ray_len),
            (center_x - width, center_y)
        ]
        draw.polygon(pts, fill=fill_col)
        # horizontal
        pts_h = [
            (center_x - ray_len, center_y),
            (center_x, center_y + width),
            (center_x + ray_len, center_y),
            (center_x, center_y - width)
        ]
        draw.polygon(pts_h, fill=fill_col)

    draw_star(star_x, star_y, int(size * 0.09), int(size * 0.022), (254, 240, 138, 255))
    draw_star(star_x, star_y, int(size * 0.05), int(size * 0.012), (255, 255, 255, 255))
    draw.ellipse([star_x - 8, star_y - 8, star_x + 8, star_y + 8], fill=(255, 255, 255, 255))
    
    # Floating spark particles
    draw.ellipse([star_x - int(size*0.22), star_y + int(size*0.06), star_x - int(size*0.22) + 12, star_y + int(size*0.06) + 12], fill=(253, 230, 138, 220))
    draw.ellipse([star_x + int(size*0.24), star_y + int(size*0.08), star_x + int(size*0.24) + 10, star_y + int(size*0.08) + 10], fill=(56, 189, 248, 230))
    draw.ellipse([star_x + int(size*0.30), star_y + int(size*0.20), star_x + int(size*0.30) + 8, star_y + int(size*0.20) + 8], fill=(253, 230, 138, 160))
    draw.ellipse([star_x - int(size*0.28), star_y + int(size*0.18), star_x - int(size*0.28) + 6, star_y + int(size*0.18) + 6], fill=(168, 85, 247, 180))

    # Open 3D Storybook
    book_cy = int(size * 0.68)
    
    # 1. Dark leather book cover bottom / drop shadow
    cover_shadow = [
        (cx - int(size*0.33), book_cy - int(size*0.06)),
        (cx, book_cy + int(size*0.01)),
        (cx + int(size*0.33), book_cy - int(size*0.06)),
        (cx + int(size*0.30), book_cy + int(size*0.15)),
        (cx, book_cy + int(size*0.21)),
        (cx - int(size*0.30), book_cy + int(size*0.15))
    ]
    draw.polygon(cover_shadow, fill=(69, 26, 3, 220))
    
    # 2. Hardcover Rim (Rich Gold Amber)
    cover_rim = [
        (cx - int(size*0.32), book_cy - int(size*0.08)),
        (cx, book_cy - int(size*0.01)),
        (cx + int(size*0.32), book_cy - int(size*0.08)),
        (cx + int(size*0.29), book_cy + int(size*0.13)),
        (cx, book_cy + int(size*0.19)),
        (cx - int(size*0.29), book_cy + int(size*0.13))
    ]
    draw.polygon(cover_rim, fill=(180, 83, 9, 255))
    
    # 3. Left Page (Soft Warm White with curvature)
    left_page = [
        (cx - 2, book_cy - int(size*0.03)),
        (cx - int(size*0.12), book_cy - int(size*0.08)),
        (cx - int(size*0.29), book_cy - int(size*0.12)),
        (cx - int(size*0.29), book_cy + int(size*0.06)),
        (cx - int(size*0.12), book_cy + int(size*0.10)),
        (cx - 2, book_cy + int(size*0.15))
    ]
    draw.polygon(left_page, fill=(254, 243, 199, 255))

    # Left Page inner shading
    left_shade = [
        (cx - 2, book_cy - int(size*0.03)),
        (cx - int(size*0.05), book_cy - int(size*0.05)),
        (cx - int(size*0.05), book_cy + int(size*0.13)),
        (cx - 2, book_cy + int(size*0.15))
    ]
    draw.polygon(left_shade, fill=(245, 158, 11, 60))

    # 4. Right Page (Pure Luminous Warm Ivory)
    right_page = [
        (cx + 2, book_cy - int(size*0.03)),
        (cx + int(size*0.12), book_cy - int(size*0.08)),
        (cx + int(size*0.29), book_cy - int(size*0.12)),
        (cx + int(size*0.29), book_cy + int(size*0.06)),
        (cx + int(size*0.12), book_cy + int(size*0.10)),
        (cx + 2, book_cy + int(size*0.15))
    ]
    draw.polygon(right_page, fill=(255, 251, 235, 255))
    
    # Right Page inner shading
    right_shade = [
        (cx + 2, book_cy - int(size*0.03)),
        (cx + int(size*0.05), book_cy - int(size*0.05)),
        (cx + int(size*0.05), book_cy + int(size*0.13)),
        (cx + 2, book_cy + int(size*0.15))
    ]
    draw.polygon(right_shade, fill=(245, 158, 11, 40))

    # 5. Book Center Spine Line
    draw.line([(cx, book_cy - int(size*0.03)), (cx, book_cy + int(size*0.15))], fill=(217, 119, 6, 255), width=6)

    # 6. Pink Ribbon Bookmark
    ribbon = [
        (cx, book_cy - int(size*0.02)),
        (cx + int(size*0.04), book_cy + int(size*0.08)),
        (cx + int(size*0.08), book_cy + int(size*0.18)),
        (cx + int(size*0.04), book_cy + int(size*0.21)),
        (cx, book_cy + int(size*0.16))
    ]
    draw.polygon(ribbon, fill=(236, 72, 153, 255))

    # 7. Luminous Sound Wave Arcs rising from the book
    wave_r = int(size * 0.18)
    wave_box = [cx - wave_r, book_cy - int(size*0.26), cx + wave_r, book_cy + int(size*0.08)]
    draw.arc(wave_box, start=200, end=340, fill=(245, 158, 11, 230), width=8)

    wave_r2 = int(size * 0.12)
    wave_box2 = [cx - wave_r2, book_cy - int(size*0.20), cx + wave_r2, book_cy + int(size*0.04)]
    draw.arc(wave_box2, start=205, end=335, fill=(56, 189, 248, 220), width=6)

    # Composite all together
    final_img = Image.alpha_composite(img, fg)
    
    # Clean cut mask
    final_clipped = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    final_clipped.paste(final_img, (0, 0), mask)
    return final_clipped

# Target density map
densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

base_dir = "/data/data/com.termux/files/home/crew-story/app/src/main/res"

print("Generating Scheme A App Icons (1024px supersampled)...")
master_squircle = create_base_icon(1024, is_round=False)
master_round = create_base_icon(1024, is_round=True)

for folder, dim in densities.items():
    target_dir = f"{base_dir}/{folder}"
    # 1. ic_launcher.png (squircle)
    out_sq = master_squircle.resize((dim, dim), Image.Resampling.LANCZOS)
    out_sq.save(f"{target_dir}/ic_launcher.png", "PNG")
    
    # 2. ic_launcher_round.png (round)
    out_rd = master_round.resize((dim, dim), Image.Resampling.LANCZOS)
    out_rd.save(f"{target_dir}/ic_launcher_round.png", "PNG")
    print(f"Generated {folder}: {dim}x{dim} ic_launcher.png & ic_launcher_round.png")

print("Icon generation completed successfully!")
