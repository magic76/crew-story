from PIL import Image
import sys
sys.path.append("/data/data/com.termux/files/home/crew-story")
from generate_icons import create_base_icon

icon_512 = create_base_icon(512, is_round=False)
icon_512.save("/data/data/com.termux/files/home/crew-story/assets/icon.png", "PNG")

icon_round_512 = create_base_icon(512, is_round=True)
icon_round_512.save("/data/data/com.termux/files/home/crew-story/assets/icon_round.png", "PNG")

print("Assets icon.png and icon_round.png created in assets/")
