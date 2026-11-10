package main

import (
	"fmt"
	"image"
	"image/color"
	"image/png"
	"os"
	"path/filepath"
)

const (
	tileSize = 16
	columns  = 8
)

var (
	grass       = color.RGBA{R: 92, G: 124, B: 73, A: 255}
	grassLight  = color.RGBA{R: 105, G: 139, B: 81, A: 255}
	grassDark   = color.RGBA{R: 77, G: 106, B: 65, A: 255}
	road        = color.RGBA{R: 198, G: 139, B: 82, A: 255}
	roadLight   = color.RGBA{R: 224, G: 169, B: 103, A: 255}
	roadEdge    = color.RGBA{R: 130, G: 87, B: 58, A: 255}
	roadShadow  = color.RGBA{R: 78, G: 63, B: 48, A: 255}
	flowerWhite = color.RGBA{R: 236, G: 219, B: 160, A: 255}
)

func main() {
	output := filepath.Join("src", "main", "assets", "tilesets", "hamlet.png")
	if err := os.MkdirAll(filepath.Dir(output), 0o755); err != nil {
		fail(err)
	}

	atlas := image.NewRGBA(image.Rect(0, 0, columns*tileSize, 2*tileSize))
	drawGrass(atlas, 0, grass, false)
	drawGrass(atlas, 1, grassLight, true)
	drawFlowers(atlas, 2)
	drawWeeds(atlas, 3)
	drawRoad(atlas, 4, false, false, false, true)
	drawRoad(atlas, 5, true, false, true, false)
	drawRoad(atlas, 6, true, true, true, true)
	drawRoad(atlas, 7, true, true, false, false)
	drawRoad(atlas, 8, true, false, false, true)
	drawRoad(atlas, 9, false, true, true, false)
	drawRoad(atlas, 10, false, false, true, true)
	drawWater(atlas, 11)
	drawStone(atlas, 12)
	drawSoil(atlas, 13)
	drawCrops(atlas, 14)
	drawGrassTuft(atlas, 15)

	file, err := os.Create(output)
	if err != nil {
		fail(err)
	}
	defer file.Close()
	if err := png.Encode(file, atlas); err != nil {
		fail(err)
	}
	fmt.Printf("Wrote %s (%dx%d, %d tiles)\n", output, atlas.Bounds().Dx(), atlas.Bounds().Dy(), 2*columns)
}

func fail(err error) {
	fmt.Fprintln(os.Stderr, err)
	os.Exit(1)
}

func tilePixel(atlas *image.RGBA, tile, x, y int, c color.RGBA) {
	tx := tile % columns
	ty := tile / columns
	atlas.SetRGBA(tx*tileSize+x, ty*tileSize+y, c)
}

func rect(atlas *image.RGBA, tile, x, y, width, height int, c color.RGBA) {
	for py := y; py < y+height; py++ {
		for px := x; px < x+width; px++ {
			tilePixel(atlas, tile, px, py, c)
		}
	}
}

func drawGrass(atlas *image.RGBA, tile int, base color.RGBA, variant bool) {
	rect(atlas, tile, 0, 0, tileSize, tileSize, base)
	for y := 1; y < tileSize; y += 5 {
		for x := (y * 3) % 7; x < tileSize; x += 8 {
			c := grassDark
			if (x+y)%3 == 0 {
				c = grassLight
			}
			if variant && (x+y)%2 == 0 {
				c = grass
			}
			rect(atlas, tile, x, y, 2, 1, c)
		}
	}
}

func drawFlowers(atlas *image.RGBA, tile int) {
	rect(atlas, tile, 3, 10, 2, 3, color.RGBA{R: 54, G: 91, B: 54, A: 255})
	rect(atlas, tile, 2, 8, 2, 2, flowerWhite)
	rect(atlas, tile, 4, 7, 2, 2, color.RGBA{R: 239, G: 190, B: 89, A: 255})
	rect(atlas, tile, 11, 3, 2, 2, color.RGBA{R: 239, G: 190, B: 89, A: 255})
	rect(atlas, tile, 10, 4, 1, 2, grassLight)
	rect(atlas, tile, 8, 13, 2, 2, flowerWhite)
	rect(atlas, tile, 7, 14, 2, 1, grassDark)
}

func drawWeeds(atlas *image.RGBA, tile int) {
	rect(atlas, tile, 3, 9, 1, 4, grassDark)
	rect(atlas, tile, 2, 8, 1, 2, grassLight)
	rect(atlas, tile, 4, 7, 1, 2, grassLight)
	rect(atlas, tile, 10, 3, 1, 4, grassDark)
	rect(atlas, tile, 9, 2, 1, 2, grassLight)
	rect(atlas, tile, 11, 1, 1, 2, grassLight)
}

func roadContains(x, y, margin int, north, east, south, west bool) bool {
	if x >= 4-margin && x < 12+margin && y >= 4-margin && y < 12+margin {
		return true
	}
	return north && x >= 4-margin && x < 12+margin && y < 8 ||
		east && x >= 8 && y >= 4-margin && y < 12+margin ||
		south && x >= 4-margin && x < 12+margin && y >= 8 ||
		west && x < 8 && y >= 4-margin && y < 12+margin
}

func drawRoad(atlas *image.RGBA, tile int, north, east, south, west bool) {
	for y := 0; y < tileSize; y++ {
		for x := 0; x < tileSize; x++ {
			switch {
			case roadContains(x, y, 2, north, east, south, west) &&
				!roadContains(x, y, 0, north, east, south, west):
				tilePixel(atlas, tile, x, y, roadShadow)
			case roadContains(x, y, 0, north, east, south, west):
				c := road
				if (x*7+y*11)%19 == 0 {
					c = roadLight
				}
				if y == 4 || x == 4 {
					c = roadLight
				}
				tilePixel(atlas, tile, x, y, c)
			}
		}
	}
}

func drawWater(atlas *image.RGBA, tile int) {
	rect(atlas, tile, 0, 0, tileSize, tileSize, color.RGBA{R: 47, G: 116, B: 133, A: 255})
	rect(atlas, tile, 2, 3, 5, 1, color.RGBA{R: 85, G: 160, B: 166, A: 255})
	rect(atlas, tile, 10, 7, 4, 1, color.RGBA{R: 85, G: 160, B: 166, A: 255})
	rect(atlas, tile, 4, 12, 3, 1, color.RGBA{R: 66, G: 141, B: 153, A: 255})
}

func drawStone(atlas *image.RGBA, tile int) {
	base := color.RGBA{R: 91, G: 93, B: 91, A: 255}
	rect(atlas, tile, 0, 0, tileSize, tileSize, base)
	for y := 1; y < tileSize; y += 5 {
		offset := 0
		if y%2 == 0 {
			offset = 4
		}
		for x := offset; x < tileSize; x += 8 {
			rect(atlas, tile, x, y, 6, 3, color.RGBA{R: 119, G: 117, B: 108, A: 255})
			rect(atlas, tile, x, y+2, 6, 1, color.RGBA{R: 68, G: 73, B: 72, A: 255})
		}
	}
}

func drawSoil(atlas *image.RGBA, tile int) {
	rect(atlas, tile, 0, 0, tileSize, tileSize, color.RGBA{R: 125, G: 81, B: 54, A: 255})
	for y := 2; y < tileSize; y += 5 {
		rect(atlas, tile, 0, y, tileSize, 1, color.RGBA{R: 157, G: 105, B: 64, A: 255})
	}
}

func drawCrops(atlas *image.RGBA, tile int) {
	stem := color.RGBA{R: 56, G: 92, B: 49, A: 255}
	leaf := color.RGBA{R: 139, G: 158, B: 74, A: 255}
	for _, x := range []int{3, 8, 13} {
		rect(atlas, tile, x, 4, 1, 10, stem)
		rect(atlas, tile, x-2, 6, 2, 2, leaf)
		rect(atlas, tile, x+1, 9, 2, 2, leaf)
		rect(atlas, tile, x-1, 2, 2, 2, color.RGBA{R: 221, G: 183, B: 91, A: 255})
	}
}

func drawGrassTuft(atlas *image.RGBA, tile int) {
	leaf := color.RGBA{R: 134, G: 157, B: 84, A: 255}
	rect(atlas, tile, 6, 10, 1, 4, grassDark)
	rect(atlas, tile, 3, 7, 1, 5, leaf)
	rect(atlas, tile, 5, 5, 1, 6, leaf)
	rect(atlas, tile, 7, 7, 1, 5, leaf)
	rect(atlas, tile, 9, 9, 1, 4, leaf)
}
