package main

import (
	"flag"
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

const (
	mapWidth  = 64
	mapHeight = 40
)

type mapObject struct {
	class               string
	name                string
	x, y, width, height int
}

func main() {
	replace := flag.Bool("replace", false, "replace the starter map with the fixed hamlet layout")
	flag.Parse()

	output := filepath.Join("src", "main", "assets", "maps", "hamlet.tmx")
	if _, err := os.Stat(output); err == nil && !*replace {
		fmt.Fprintf(os.Stderr, "%s already exists; edit it in Tiled or pass -replace to restore the starter layout\n", output)
		os.Exit(1)
	}
	if err := os.MkdirAll(filepath.Dir(output), 0o755); err != nil {
		fail(err)
	}

	ground := make([]int, mapWidth*mapHeight)
	paths := make([]int, mapWidth*mapHeight)
	details := make([]int, mapWidth*mapHeight)
	for i := range ground {
		ground[i] = 1
	}

	// Deliberate fixed geography: a stream and mill pond, one shared green,
	// clustered cottages and workshops, with fields and woodland on the edge.
	fillRect(ground, 46, 0, 8, 2, 12)
	fillRect(ground, 48, 2, 8, 2, 12)
	fillRect(ground, 50, 4, 7, 2, 12)
	fillRect(ground, 51, 6, 6, 2, 12)
	fillRect(ground, 52, 8, 5, 2, 12)
	fillRect(ground, 50, 10, 7, 2, 12)
	fillRect(ground, 48, 12, 8, 2, 12)
	fillRect(ground, 46, 14, 8, 2, 12)
	fillRect(ground, 44, 16, 8, 2, 12)
	fillRect(ground, 43, 18, 7, 2, 12)
	fillRect(ground, 42, 20, 7, 2, 12)
	fillRect(ground, 42, 21, 7, 2, 12)
	fillRect(ground, 42, 22, 7, 2, 12)
	fillRect(ground, 44, 23, 7, 2, 12)
	fillRect(ground, 46, 24, 7, 2, 12)

	// Shared green around the crossing; the lane network follows existing
	// footpaths rather than cutting through yards or buildings.
	fillRect(ground, 25, 17, 14, 9, 2)
	fillRect(ground, 28, 18, 8, 7, 1)
	horizontalPath(paths, 3, 60, 21)
	verticalPath(paths, 31, 3, 36)
	horizontalPath(paths, 25, 31, 14)
	horizontalPath(paths, 31, 40, 28)
	horizontalPath(paths, 40, 49, 18)
	horizontalPath(paths, 40, 48, 26)
	verticalPath(paths, 17, 14, 18)
	verticalPath(paths, 45, 14, 18)
	verticalPath(paths, 21, 22, 28)
	verticalPath(paths, 38, 22, 27)
	verticalPath(paths, 50, 24, 29)
	verticalPath(paths, 54, 28, 33)

	// A fenced working farm and small kitchen gardens, separate from the common.
	fillRect(ground, 48, 29, 12, 8, 14)
	for y := 30; y <= 35; y += 2 {
		for x := 49; x <= 58; x += 2 {
			details[y*mapWidth+x] = 15
		}
	}
	fillRect(ground, 3, 29, 5, 4, 14)
	for y := 29; y < 33; y++ {
		for x := 3; x < 8; x++ {
			if (x+y)%3 == 0 {
				details[y*mapWidth+x] = 15
			}
		}
	}
	fillRect(ground, 54, 33, 6, 4, 14)
	fillRect(ground, 14, 4, 5, 4, 13)

	// Fixed garden details and meadow edges; no per-playthrough random layout.
	for _, point := range [][2]int{
		{5, 10}, {10, 6}, {13, 12}, {19, 9}, {23, 5}, {27, 11}, {36, 7},
		{39, 10}, {42, 5}, {57, 8}, {8, 18}, {12, 25}, {18, 31}, {24, 34},
		{28, 29}, {36, 33}, {41, 30}, {60, 24}, {9, 36}, {16, 37}, {24, 7},
		{38, 5}, {59, 12}, {6, 22}, {57, 37}, {47, 37}, {33, 37}, {2, 15},
	} {
		details[point[1]*mapWidth+point[0]] = 3
	}
	for _, point := range [][2]int{
		{7, 13}, {12, 10}, {20, 8}, {24, 12}, {37, 10}, {41, 7}, {58, 15},
		{10, 27}, {18, 35}, {25, 32}, {36, 30}, {41, 35}, {61, 19}, {2, 25},
	} {
		details[point[1]*mapWidth+point[0]] = 4
	}

	objects := []mapObject{
		{"building", "The Golden Stag Tavern", 448, 224, 48, 40},
		{"building", "Village Blacksmith", 336, 224, 48, 40},
		{"building", "Village Bakery", 544, 224, 48, 40},
		{"building", "Village Chapel", 784, 256, 48, 48},
		{"building", "Carpenter Workshop", 256, 368, 48, 40},
		{"building", "Farmstead", 752, 400, 56, 48},
		{"building", "Market Stalls", 448, 368, 56, 32},
		{"building", "North Cottage", 432, 112, 48, 40},
		{"building", "East Cottage", 592, 144, 48, 40},
		{"building", "West Cottage", 192, 400, 48, 40},
		{"building", "South Cottage", 432, 496, 48, 40},
		{"building", "Weaver Cottage", 608, 336, 48, 40},
		{"bridge", "Oak Footbridge", 672, 336, 112, 16},
		{"tree", "West Oak 1", 32, 32, 24, 32},
		{"tree", "West Oak 2", 64, 64, 24, 32},
		{"tree", "West Oak 3", 32, 112, 24, 32},
		{"tree", "West Oak 4", 80, 160, 24, 32},
		{"tree", "West Oak 5", 48, 224, 24, 32},
		{"tree", "West Oak 6", 96, 304, 24, 32},
		{"tree", "West Oak 7", 144, 496, 24, 32},
		{"tree", "West Oak 8", 80, 544, 24, 32},
		{"tree", "South Oak 1", 288, 560, 24, 32},
		{"tree", "South Oak 2", 368, 576, 24, 32},
		{"tree", "South Oak 3", 560, 560, 24, 32},
		{"tree", "East Oak 1", 912, 464, 24, 32},
		{"tree", "East Oak 2", 944, 384, 24, 32},
		{"tree", "East Oak 3", 880, 320, 24, 32},
		{"tree", "North Oak 1", 240, 32, 24, 32},
		{"tree", "North Oak 2", 320, 64, 24, 32},
		{"tree", "North Oak 3", 400, 32, 24, 32},
		{"tree", "North Oak 4", 608, 32, 24, 32},
		{"tree", "North Oak 5", 704, 64, 24, 32},
		{"tree", "North Oak 6", 832, 48, 24, 32},
		{"villager", "Aldric", 392, 327, 16, 24},
		{"villager", "Eliza", 504, 320, 16, 24},
		{"villager", "Thomas", 608, 343, 16, 24},
		{"villager", "Eleanor", 416, 391, 16, 24},
		{"villager", "Roland", 544, 391, 16, 24},
		{"villager", "Mira", 656, 343, 16, 24},
		{"villager", "Edmund", 288, 343, 16, 24},
		{"villager", "Beatrice", 480, 439, 16, 24},
		{"villager", "Father Oswin", 720, 343, 16, 24},
		{"villager", "Agnes", 368, 455, 16, 24},
		{"villager", "Finn", 816, 439, 16, 24},
		{"villager", "Margaret", 448, 471, 16, 24},
	}

	var tmx strings.Builder
	tmx.WriteString(`<?xml version="1.0" encoding="UTF-8"?>
<map version="1.8" tiledversion="1.8.2" orientation="orthogonal" renderorder="right-down" width="64" height="40" tilewidth="16" tileheight="16" infinite="0" backgroundcolor="#5c7c49" nextlayerid="5" nextobjectid="`)
	fmt.Fprintf(&tmx, "%d", len(objects)+1)
	tmx.WriteString(`">
 <tileset firstgid="1" name="Hamlet Ground" tilewidth="16" tileheight="16" tilecount="16" columns="8">
  <image source="../tilesets/hamlet.png" width="128" height="32"/>
 </tileset>
`)
	writeLayer(&tmx, 1, "Grass, water and field", ground)
	writeLayer(&tmx, 2, "Footpaths", paths)
	writeLayer(&tmx, 3, "Wildflowers and crops", details)
	tmx.WriteString(` <objectgroup id="4" name="Buildings, trees and villagers" draworder="topdown">
`)
	for id, object := range objects {
		fmt.Fprintf(&tmx, "  <object id=\"%d\" name=\"%s\" class=\"%s\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"",
			id+1, object.name, object.class, object.x, object.y, object.width, object.height)
		if object.class == "building" {
			fmt.Fprintf(&tmx, ">\n   <properties><property name=\"interior-map\" value=\"maps/interiors/%s.tmx\"/></properties>\n  </object>\n",
				interiorMap(object.name))
		} else {
			tmx.WriteString("/>\n")
		}
	}
	tmx.WriteString(` </objectgroup>
</map>
`)

	if err := os.WriteFile(output, []byte(tmx.String()), 0o644); err != nil {
		fail(err)
	}
	fmt.Printf("Wrote fixed %dx%d-tile hamlet with %d buildings and %d villagers to %s\n",
		mapWidth, mapHeight, countObjects(objects, "building"), countObjects(objects, "villager"), output)
}

func interiorMap(building string) string {
	switch {
	case strings.Contains(strings.ToLower(building), "tavern"):
		return "tavern"
	case strings.Contains(strings.ToLower(building), "smith"):
		return "smithy"
	case strings.Contains(strings.ToLower(building), "bakery"):
		return "bakery"
	case strings.Contains(strings.ToLower(building), "chapel"):
		return "chapel"
	case strings.Contains(strings.ToLower(building), "farm"):
		return "farmstead"
	case strings.Contains(strings.ToLower(building), "workshop"):
		return "workshop"
	case strings.Contains(strings.ToLower(building), "market"):
		return "market"
	case strings.Contains(strings.ToLower(building), "weaver"):
		return "weaver"
	default:
		return "cottage"
	}
}

func fillRect(layer []int, x, y, width, height, gid int) {
	for row := y; row < y+height; row++ {
		for column := x; column < x+width; column++ {
			layer[row*mapWidth+column] = gid
		}
	}
}

func horizontalPath(layer []int, startX, endX, y int) {
	for x := startX; x <= endX; x++ {
		layer[y*mapWidth+x] = 5
	}
}

func verticalPath(layer []int, x, startY, endY int) {
	for y := startY; y <= endY; y++ {
		layer[y*mapWidth+x] = 6
	}
}

func writeLayer(out *strings.Builder, id int, name string, tiles []int) {
	fmt.Fprintf(out, " <layer id=\"%d\" name=\"%s\" width=\"%d\" height=\"%d\">\n  <data encoding=\"csv\">\n", id, name, mapWidth, mapHeight)
	for index, tile := range tiles {
		if index > 0 {
			if index%mapWidth == 0 {
				out.WriteByte('\n')
			} else {
				out.WriteByte(',')
			}
		}
		fmt.Fprint(out, tile)
	}
	out.WriteString("\n  </data>\n </layer>\n")
}

func countObjects(objects []mapObject, class string) int {
	count := 0
	for _, object := range objects {
		if object.class == class {
			count++
		}
	}
	return count
}

func fail(err error) {
	fmt.Fprintln(os.Stderr, err)
	os.Exit(1)
}
