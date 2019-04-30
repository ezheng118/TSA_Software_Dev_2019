from fpdf import FPDF
import argparse as ap
from PIL import Image


parser = ap.ArgumentParser()
parser.add_argument("-i", "--image", required = True, help = "path of image to be converted to pdf")

arg = vars(parser.parse_args())

image_name = arg["image"]

im = Image.open(image_name)
width, height = im.size

pdf = FPDF(unit = "pt", format = [width, height])
pdf.add_page()
pdf.image(image_name, 0, 0)
pdf.output("image_scan.pdf", "F")