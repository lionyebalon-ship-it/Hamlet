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
	rows     = 12
)

var (
	ink       = color.RGBA{R: 48, G: 37, B: 31, A: 255}
	woodDark  = color.RGBA{R: 76, G: 48, B: 34, A: 255}
	wood      = color.RGBA{R: 126, G: 79, B: 49, A: 255}
	woodLight = color.RGBA{R: 166, G: 113, B: 69, A: 255}
	woodGlow  = color.RGBA{R: 197, G: 145, B: 90, A: 255}
	plaster   = color.RGBA{R: 206, G: 177, B: 127, A: 255}
	cream     = color.RGBA{R: 229, G: 204, B: 154, A: 255}
	shadow    = color.RGBA{R: 91, G: 67, B: 48, A: 255}
	iron      = color.RGBA{R: 72, G: 76, B: 72, A: 255}
	ironLight = color.RGBA{R: 139, G: 139, B: 120, A: 255}
	fire      = color.RGBA{R: 231, G: 119, B: 52, A: 255}
	fireLight = color.RGBA{R: 249, G: 194, B: 79, A: 255}
)

func main() {
	output := filepath.Join("src", "main", "assets", "tilesets", "hamlet-interiors.png")
	if err := os.MkdirAll(filepath.Dir(output), 0o755); err != nil {
		fail(err)
	}
	atlas := image.NewRGBA(image.Rect(0, 0, columns*tileSize, rows*tileSize))
	drawFloor(atlas, 0, color.RGBA{R: 139, G: 88, B: 53, A: 255})
	drawFloor(atlas, 1, color.RGBA{R: 151, G: 96, B: 57, A: 255})
	drawFloor(atlas, 2, color.RGBA{R: 119, G: 76, B: 50, A: 255})
	drawFloor(atlas, 3, color.RGBA{R: 166, G: 110, B: 67, A: 255})
	drawWall(atlas, 4)
	drawTimberWall(atlas, 5)
	drawWindow(atlas, 6)
	drawDoor(atlas, 7)
	drawBed(atlas, 8)
	drawTable(atlas, 12)
	drawStool(atlas, 16)
	drawBarrel(atlas, 17)
	drawShelf(atlas, 18)
	drawHearth(atlas, 20)
	drawCounter(atlas, 24)
	drawAnvil(atlas, 28)
	drawWorkbench(atlas, 29)
	drawLoom(atlas, 33)
	drawAltar(atlas, 37)
	drawPew(atlas, 39)
	drawCrate(atlas, 41)
	drawOven(atlas, 42)
	drawBench(atlas, 46)
	drawEarthFloor(atlas, 48)
	drawStoneFloor(atlas, 52)
	drawBrickFloor(atlas, 56)
	drawRushFloor(atlas, 60)
	drawChest(atlas, 64)
	drawSpinningWheel(atlas, 65)
	drawGrainSacks(atlas, 69)
	drawPottery(atlas, 73)
	drawStrawBed(atlas, 77)
	drawWashTub(atlas, 81)
	drawToolRack(atlas, 82)
	drawBasket(atlas, 84)
	drawHerbRack(atlas, 85)
	drawFlourSack(atlas, 86)
	drawMilkChurn(atlas, 87)
	drawFirewood(atlas, 88)
	drawWoodStack(atlas, 89)
	drawFishRack(atlas, 91)
	drawBookStand(atlas, 93)

	file, err := os.Create(output)
	if err != nil {
		fail(err)
	}
	defer file.Close()
	if err := png.Encode(file, atlas); err != nil {
		fail(err)
	}
	fmt.Printf("Wrote %s (%dx%d pixels, %d furniture/floor tiles)\n",
		output, atlas.Bounds().Dx(), atlas.Bounds().Dy(), columns*rows)
}

func set(atlas *image.RGBA, tile, x, y int, c color.RGBA) {
	atlas.SetRGBA((tile%columns)*tileSize+x, (tile/columns)*tileSize+y, c)
}

func rect(atlas *image.RGBA, tile, x, y, width, height int, c color.RGBA) {
	for py := y; py < y+height; py++ {
		for px := x; px < x+width; px++ {
			set(atlas, tile, px, py, c)
		}
	}
}

func drawFloor(atlas *image.RGBA, tile int, base color.RGBA) {
	rect(atlas, tile, 0, 0, 16, 16, base)
	for y := 0; y < 16; y += 4 {
		rect(atlas, tile, 0, y, 16, 1, woodDark)
		offset := 0
		if y%8 == 0 {
			offset = 7
		}
		rect(atlas, tile, offset, y, 1, 4, woodDark)
		rect(atlas, tile, 2, y+1, 3, 1, woodGlow)
	}
}

func drawEarthFloor(atlas *image.RGBA, start int) {
	bases := []color.RGBA{
		{R: 111, G: 83, B: 53, A: 255},
		{R: 120, G: 91, B: 59, A: 255},
		{R: 105, G: 78, B: 51, A: 255},
		{R: 116, G: 87, B: 56, A: 255},
	}
	for part, base := range bases {
		rect(atlas, start+part, 0, 0, 16, 16, base)
		rect(atlas, start+part, 2+part%3*4, 3+part%2*6, 2, 1, shadow)
		rect(atlas, start+part, 10-part%3*3, 11-part%2*5, 1, 2, woodDark)
		rect(atlas, start+part, 6, 7+part%2, 2, 1, plaster)
	}
}

func drawStoneFloor(atlas *image.RGBA, start int) {
	for part := 0; part < 4; part++ {
		base := color.RGBA{R: 112 + uint8(part*4), G: 112 + uint8(part*4), B: 99 + uint8(part*3), A: 255}
		rect(atlas, start+part, 0, 0, 16, 16, base)
		rect(atlas, start+part, 0, 0, 16, 1, ink)
		rect(atlas, start+part, 0, 8, 16, 1, ink)
		rect(atlas, start+part, 0, 15, 16, 1, ink)
		offset := (part % 2) * 6
		rect(atlas, start+part, offset, 0, 1, 8, ink)
		rect(atlas, start+part, 8+offset/2, 8, 1, 7, ink)
		rect(atlas, start+part, 2, 3, 3, 1, ironLight)
	}
}

func drawBrickFloor(atlas *image.RGBA, start int) {
	for part := 0; part < 4; part++ {
		rect(atlas, start+part, 0, 0, 16, 16, color.RGBA{R: 127, G: 78, B: 54, A: 255})
		for y := 0; y < 16; y += 5 {
			rect(atlas, start+part, 0, y, 16, 1, woodDark)
			offset := (part % 2) * 4
			rect(atlas, start+part, offset, y, 1, 5, woodDark)
			rect(atlas, start+part, offset+7, y, 1, 5, woodDark)
		}
		rect(atlas, start+part, 2, 2+(part%2)*5, 3, 1, woodLight)
	}
}

func drawRushFloor(atlas *image.RGBA, start int) {
	for part := 0; part < 4; part++ {
		rect(atlas, start+part, 0, 0, 16, 16, color.RGBA{R: 132, G: 112, B: 72, A: 255})
		for x := 1 + part%2; x < 16; x += 4 {
			rect(atlas, start+part, x, 1, 1, 14, color.RGBA{R: 154, G: 133, B: 87, A: 255})
		}
		rect(atlas, start+part, 0, 0, 16, 1, woodDark)
		rect(atlas, start+part, 0, 15, 16, 1, woodDark)
	}
}

func drawWall(atlas *image.RGBA, tile int) {
	rect(atlas, tile, 0, 0, 16, 16, plaster)
	rect(atlas, tile, 0, 0, 16, 2, woodDark)
	rect(atlas, tile, 0, 2, 16, 1, wood)
	rect(atlas, tile, 0, 13, 16, 3, woodDark)
	rect(atlas, tile, 2, 3, 1, 10, cream)
	rect(atlas, tile, 13, 3, 1, 10, shadow)
	rect(atlas, tile, 3, 11, 10, 1, color.RGBA{R: 189, G: 156, B: 109, A: 255})
}

func drawTimberWall(atlas *image.RGBA, tile int) {
	drawWall(atlas, tile)
	rect(atlas, tile, 0, 3, 16, 2, wood)
	rect(atlas, tile, 0, 10, 16, 2, wood)
	rect(atlas, tile, 2, 4, 12, 1, woodLight)
	rect(atlas, tile, 2, 11, 12, 1, woodLight)
}

func drawWindow(atlas *image.RGBA, tile int) {
	drawWall(atlas, tile)
	rect(atlas, tile, 3, 4, 10, 7, woodDark)
	rect(atlas, tile, 4, 5, 8, 5, color.RGBA{R: 102, G: 155, B: 160, A: 255})
	rect(atlas, tile, 7, 5, 2, 5, woodLight)
	rect(atlas, tile, 4, 7, 8, 1, woodLight)
	rect(atlas, tile, 2, 11, 12, 2, wood)
}

func drawDoor(atlas *image.RGBA, tile int) {
	drawFloor(atlas, tile, color.RGBA{R: 143, G: 92, B: 57, A: 255})
	rect(atlas, tile, 1, 0, 14, 3, woodDark)
	rect(atlas, tile, 2, 3, 12, 10, wood)
	rect(atlas, tile, 4, 4, 1, 8, woodLight)
	rect(atlas, tile, 11, 4, 1, 8, woodDark)
	rect(atlas, tile, 12, 8, 1, 1, fireLight)
	rect(atlas, tile, 0, 13, 16, 3, ink)
}

func drawBed(atlas *image.RGBA, start int) {
	colors := []color.RGBA{woodDark, wood, color.RGBA{R: 119, G: 68, B: 69, A: 255}, color.RGBA{R: 159, G: 104, B: 90, A: 255}}
	for part := 0; part < 4; part++ {
		rect(atlas, start+part, 1, 1, 14, 14, shadow)
		rect(atlas, start+part, 2, 2, 12, 12, colors[part])
	}
	rect(atlas, start, 2, 2, 12, 4, cream)
	rect(atlas, start+1, 2, 2, 12, 4, cream)
	rect(atlas, start, 3, 3, 5, 2, plaster)
	rect(atlas, start+1, 8, 3, 5, 2, plaster)
	rect(atlas, start+2, 2, 8, 12, 1, woodDark)
	rect(atlas, start+3, 2, 8, 12, 1, woodDark)
}

func drawTable(atlas *image.RGBA, start int) {
	for part := 0; part < 4; part++ {
		rect(atlas, start+part, 1, 2, 14, 11, woodDark)
		rect(atlas, start+part, 2, 2, 12, 9, wood)
		rect(atlas, start+part, 3, 3, 10, 1, woodGlow)
		rect(atlas, start+part, 2, 12, 2, 3, woodDark)
		rect(atlas, start+part, 12, 12, 2, 3, woodDark)
	}
	rect(atlas, start, 7, 6, 2, 2, cream)
	rect(atlas, start+1, 7, 6, 2, 2, iron)
	rect(atlas, start+2, 5, 5, 2, 2, woodLight)
}

func drawStool(atlas *image.RGBA, tile int) {
	rect(atlas, tile, 2, 4, 12, 7, woodDark)
	rect(atlas, tile, 3, 4, 10, 5, woodLight)
	rect(atlas, tile, 4, 5, 8, 1, woodGlow)
	rect(atlas, tile, 4, 11, 2, 4, woodDark)
	rect(atlas, tile, 10, 11, 2, 4, woodDark)
}

func drawBarrel(atlas *image.RGBA, tile int) {
	rect(atlas, tile, 3, 2, 10, 12, wood)
	rect(atlas, tile, 4, 1, 8, 2, woodDark)
	rect(atlas, tile, 4, 13, 8, 2, woodDark)
	rect(atlas, tile, 2, 4, 12, 1, iron)
	rect(atlas, tile, 2, 10, 12, 1, iron)
	rect(atlas, tile, 5, 4, 2, 6, woodGlow)
}

func drawShelf(atlas *image.RGBA, start int) {
	for part := 0; part < 2; part++ {
		rect(atlas, start+part, 1, 2, 14, 12, woodDark)
		rect(atlas, start+part, 2, 3, 12, 2, woodLight)
		rect(atlas, start+part, 2, 8, 12, 2, woodLight)
		rect(atlas, start+part, 3, 5, 3, 3, color.RGBA{R: 168, G: 104, B: 54, A: 255})
		rect(atlas, start+part, 8, 5, 3, 3, color.RGBA{R: 116, G: 132, B: 111, A: 255})
		rect(atlas, start+part, 5, 10, 4, 3, color.RGBA{R: 196, G: 158, B: 92, A: 255})
	}
}

func drawHearth(atlas *image.RGBA, start int) {
	for part := 0; part < 4; part++ {
		rect(atlas, start+part, 1, 1, 14, 14, iron)
		rect(atlas, start+part, 2, 2, 12, 12, color.RGBA{R: 104, G: 99, B: 83, A: 255})
	}
	rect(atlas, start, 5, 5, 6, 7, ink)
	rect(atlas, start+1, 4, 5, 8, 7, ink)
	rect(atlas, start+2, 7, 7, 3, 4, fire)
	rect(atlas, start+3, 7, 6, 2, 4, fireLight)
}

func drawCounter(atlas *image.RGBA, start int) {
	for part := 0; part < 4; part++ {
		rect(atlas, start+part, 1, 2, 14, 12, woodDark)
		rect(atlas, start+part, 2, 2, 12, 8, woodLight)
		rect(atlas, start+part, 3, 3, 10, 2, woodGlow)
		rect(atlas, start+part, 2, 11, 12, 2, wood)
		rect(atlas, start+part, 4, 5, 1, 4, wood)
		rect(atlas, start+part, 11, 5, 1, 4, wood)
	}
	rect(atlas, start, 7, 4, 2, 2, iron)
	rect(atlas, start+1, 8, 4, 2, 2, plaster)
}

func drawAnvil(atlas *image.RGBA, tile int) {
	rect(atlas, tile, 3, 11, 10, 4, woodDark)
	rect(atlas, tile, 5, 8, 6, 4, iron)
	rect(atlas, tile, 2, 5, 12, 4, ironLight)
	rect(atlas, tile, 1, 4, 8, 2, ironLight)
	rect(atlas, tile, 8, 3, 5, 3, iron)
	rect(atlas, tile, 3, 7, 8, 1, color.RGBA{R: 184, G: 181, B: 158, A: 255})
}

func drawWorkbench(atlas *image.RGBA, start int) {
	drawCounter(atlas, start)
	drawCounter(atlas, start+1)
	rect(atlas, start, 3, 1, 10, 2, woodDark)
	rect(atlas, start+1, 3, 1, 10, 2, woodDark)
	rect(atlas, start, 5, 5, 2, 2, iron)
	rect(atlas, start+1, 8, 5, 3, 2, ironLight)
}

func drawLoom(atlas *image.RGBA, start int) {
	for part := 0; part < 4; part++ {
		rect(atlas, start+part, 2, 2, 12, 12, woodDark)
		rect(atlas, start+part, 4, 3, 2, 10, woodLight)
		rect(atlas, start+part, 10, 3, 2, 10, woodLight)
		rect(atlas, start+part, 5, 4, 6, 1, woodGlow)
		rect(atlas, start+part, 5, 7, 6, 1, woodGlow)
		rect(atlas, start+part, 5, 10, 6, 1, woodGlow)
	}
	rect(atlas, start+1, 5, 5, 6, 2, color.RGBA{R: 162, G: 105, B: 115, A: 255})
	rect(atlas, start+3, 5, 8, 6, 2, color.RGBA{R: 162, G: 105, B: 115, A: 255})
}

func drawAltar(atlas *image.RGBA, start int) {
	for part := 0; part < 2; part++ {
		rect(atlas, start+part, 3, 4, 10, 9, woodDark)
		rect(atlas, start+part, 2, 3, 12, 3, cream)
		rect(atlas, start+part, 4, 7, 8, 1, woodLight)
		rect(atlas, start+part, 4, 13, 2, 2, wood)
		rect(atlas, start+part, 10, 13, 2, 2, wood)
	}
	rect(atlas, start, 7, 0, 2, 4, color.RGBA{R: 181, G: 64, B: 59, A: 255})
	rect(atlas, start, 6, 1, 4, 1, color.RGBA{R: 181, G: 64, B: 59, A: 255})
}

func drawPew(atlas *image.RGBA, start int) {
	for part := 0; part < 2; part++ {
		rect(atlas, start+part, 2, 3, 12, 3, woodLight)
		rect(atlas, start+part, 3, 4, 10, 1, woodGlow)
		rect(atlas, start+part, 3, 7, 2, 7, woodDark)
		rect(atlas, start+part, 11, 7, 2, 7, woodDark)
		rect(atlas, start+part, 2, 13, 12, 2, wood)
	}
}

func drawCrate(atlas *image.RGBA, tile int) {
	rect(atlas, tile, 2, 2, 12, 12, woodDark)
	rect(atlas, tile, 3, 3, 10, 10, wood)
	rect(atlas, tile, 7, 3, 2, 10, woodDark)
	rect(atlas, tile, 3, 7, 10, 2, woodDark)
	rect(atlas, tile, 4, 4, 2, 2, woodGlow)
}

func drawOven(atlas *image.RGBA, start int) {
	for part := 0; part < 4; part++ {
		rect(atlas, start+part, 1, 1, 14, 14, woodDark)
		rect(atlas, start+part, 2, 2, 12, 12, color.RGBA{R: 132, G: 91, B: 64, A: 255})
	}
	rect(atlas, start, 5, 5, 6, 8, ink)
	rect(atlas, start+1, 4, 5, 8, 8, ink)
	rect(atlas, start+2, 7, 9, 3, 3, fire)
	rect(atlas, start+3, 7, 8, 2, 3, fireLight)
	rect(atlas, start, 3, 2, 10, 2, plaster)
	rect(atlas, start+1, 2, 2, 12, 2, plaster)
}

func drawBench(atlas *image.RGBA, start int) {
	for part := 0; part < 2; part++ {
		rect(atlas, start+part, 2, 4, 12, 5, woodDark)
		rect(atlas, start+part, 3, 4, 10, 3, woodLight)
		rect(atlas, start+part, 4, 9, 2, 5, wood)
		rect(atlas, start+part, 10, 9, 2, 5, wood)
	}
}

func drawChest(atlas *image.RGBA, tile int) {
	rect(atlas, tile, 2, 5, 12, 9, woodDark)
	rect(atlas, tile, 3, 4, 10, 8, wood)
	rect(atlas, tile, 4, 5, 8, 1, woodGlow)
	rect(atlas, tile, 7, 7, 2, 3, iron)
	rect(atlas, tile, 4, 12, 2, 2, ink)
	rect(atlas, tile, 10, 12, 2, 2, ink)
}

func drawSpinningWheel(atlas *image.RGBA, start int) {
	rect(atlas, start, 2, 2, 12, 2, woodDark)
	rect(atlas, start, 2, 4, 2, 10, woodDark)
	rect(atlas, start, 4, 5, 7, 1, woodLight)
	rect(atlas, start+1, 2, 2, 12, 2, woodDark)
	rect(atlas, start+1, 12, 4, 2, 10, woodDark)
	rect(atlas, start+1, 5, 5, 7, 1, woodLight)
	rect(atlas, start+2, 2, 12, 2, 2, woodDark)
	rect(atlas, start+2, 10, 2, 2, 12, woodDark)
	rect(atlas, start+2, 4, 4, 7, 1, woodLight)
	rect(atlas, start+3, 2, 12, 12, 2, woodDark)
	rect(atlas, start+3, 12, 2, 2, 12, woodDark)
	rect(atlas, start+3, 4, 4, 7, 1, woodLight)
}

func drawGrainSacks(atlas *image.RGBA, start int) {
	colors := []color.RGBA{
		{R: 170, G: 141, B: 91, A: 255},
		{R: 190, G: 160, B: 103, A: 255},
		{R: 151, G: 126, B: 82, A: 255},
		{R: 180, G: 149, B: 95, A: 255},
	}
	for part, c := range colors {
		rect(atlas, start+part, 3, 4, 10, 10, shadow)
		rect(atlas, start+part, 4, 3, 8, 10, c)
		rect(atlas, start+part, 5, 2, 6, 2, cream)
		rect(atlas, start+part, 5+part%2, 7, 2, 1, color.RGBA{R: 205, G: 181, B: 133, A: 255})
	}
}

func drawPottery(atlas *image.RGBA, start int) {
	colors := []color.RGBA{
		{R: 128, G: 81, B: 55, A: 255},
		{R: 105, G: 113, B: 91, A: 255},
		{R: 169, G: 125, B: 75, A: 255},
		{R: 123, G: 83, B: 72, A: 255},
	}
	for part, c := range colors {
		rect(atlas, start+part, 5, 3, 6, 2, woodDark)
		rect(atlas, start+part, 4, 5, 8, 7, c)
		rect(atlas, start+part, 5, 12, 6, 2, woodDark)
		rect(atlas, start+part, 6, 6, 1, 4, cream)
	}
}

func drawStrawBed(atlas *image.RGBA, start int) {
	for part := 0; part < 4; part++ {
		rect(atlas, start+part, 1, 1, 14, 14, woodDark)
		rect(atlas, start+part, 2, 2, 12, 12, color.RGBA{R: 162, G: 132, B: 78, A: 255})
		rect(atlas, start+part, 3, 4+part%2, 9, 1, cream)
		rect(atlas, start+part, 4, 8+part%2, 8, 1, woodLight)
	}
	rect(atlas, start, 2, 2, 12, 3, plaster)
	rect(atlas, start+1, 2, 2, 12, 3, plaster)
}

func drawWashTub(atlas *image.RGBA, tile int) {
	rect(atlas, tile, 3, 5, 10, 9, woodDark)
	rect(atlas, tile, 4, 5, 8, 7, color.RGBA{R: 105, G: 143, B: 151, A: 255})
	rect(atlas, tile, 3, 4, 10, 2, woodLight)
	rect(atlas, tile, 4, 12, 8, 2, wood)
}

func drawToolRack(atlas *image.RGBA, start int) {
	for part := 0; part < 2; part++ {
		rect(atlas, start+part, 2, 3, 12, 2, woodDark)
		rect(atlas, start+part, 3, 4, 10, 1, woodLight)
		rect(atlas, start+part, 4, 6, 2, 8, wood)
		rect(atlas, start+part, 8, 5, 1, 9, iron)
		rect(atlas, start+part, 11, 6, 2, 8, wood)
		rect(atlas, start+part, 10, 5, 4, 2, ironLight)
	}
}

func drawBasket(atlas *image.RGBA, tile int) {
	rect(atlas, tile, 3, 6, 10, 8, woodDark)
	rect(atlas, tile, 4, 7, 8, 6, color.RGBA{R: 174, G: 133, B: 76, A: 255})
	for y := 8; y < 13; y += 2 {
		rect(atlas, tile, 4, y, 8, 1, woodLight)
	}
	rect(atlas, tile, 5, 4, 6, 2, woodDark)
	rect(atlas, tile, 6, 5, 4, 1, woodGlow)
}

func drawHerbRack(atlas *image.RGBA, tile int) {
	rect(atlas, tile, 2, 3, 12, 2, woodDark)
	for x := 3; x < 14; x += 4 {
		rect(atlas, tile, x, 5, 2, 6, color.RGBA{R: 104, G: 133, B: 68, A: 255})
		rect(atlas, tile, x+1, 10, 1, 3, color.RGBA{R: 191, G: 167, B: 111, A: 255})
	}
}

func drawFlourSack(atlas *image.RGBA, tile int) {
	rect(atlas, tile, 4, 4, 8, 10, shadow)
	rect(atlas, tile, 5, 3, 6, 10, cream)
	rect(atlas, tile, 6, 2, 4, 2, plaster)
	rect(atlas, tile, 6, 8, 4, 1, woodLight)
}

func drawMilkChurn(atlas *image.RGBA, tile int) {
	rect(atlas, tile, 5, 4, 6, 10, woodDark)
	rect(atlas, tile, 6, 5, 4, 8, color.RGBA{R: 179, G: 160, B: 119, A: 255})
	rect(atlas, tile, 6, 3, 4, 2, woodLight)
	rect(atlas, tile, 7, 2, 2, 2, iron)
}

func drawFirewood(atlas *image.RGBA, tile int) {
	for y := 4; y < 14; y += 4 {
		rect(atlas, tile, 3, y, 10, 3, woodDark)
		rect(atlas, tile, 4, y, 8, 2, woodLight)
		rect(atlas, tile, 5, y, 1, 2, woodGlow)
	}
}

func drawWoodStack(atlas *image.RGBA, start int) {
	for part := 0; part < 2; part++ {
		rect(atlas, start+part, 1, 4, 14, 8, woodDark)
		rect(atlas, start+part, 2, 5, 12, 6, wood)
		rect(atlas, start+part, 3, 6, 10, 2, woodLight)
		rect(atlas, start+part, 2, 11, 12, 2, woodDark)
	}
}

func drawFishRack(atlas *image.RGBA, start int) {
	for part := 0; part < 2; part++ {
		rect(atlas, start+part, 2, 3, 12, 2, woodDark)
		rect(atlas, start+part, 3, 4, 10, 1, woodLight)
		rect(atlas, start+part, 4, 6, 7, 3, color.RGBA{R: 131, G: 142, B: 130, A: 255})
		rect(atlas, start+part, 5, 7, 6, 1, ironLight)
		rect(atlas, start+part, 6, 10, 5, 2, wood)
	}
}

func drawBookStand(atlas *image.RGBA, tile int) {
	rect(atlas, tile, 3, 8, 10, 5, woodDark)
	rect(atlas, tile, 4, 7, 8, 4, woodLight)
	rect(atlas, tile, 5, 4, 2, 4, color.RGBA{R: 130, G: 65, B: 55, A: 255})
	rect(atlas, tile, 7, 3, 2, 5, color.RGBA{R: 183, G: 151, B: 88, A: 255})
	rect(atlas, tile, 9, 4, 2, 4, color.RGBA{R: 93, G: 112, B: 81, A: 255})
}

func fail(err error) {
	fmt.Fprintln(os.Stderr, err)
	os.Exit(1)
}
