package main

import (
	"flag"
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

type placement struct {
	tile   int
	x      int
	y      int
	width  int
	height int
}

type room struct {
	file     string
	title    string
	theme    string
	width    int
	height   int
	floorGid int
	wallGid  int
	windowXs []int
	doorX    int
	props    []placement
}

func main() {
	replace := flag.Bool("replace", false, "replace fixed premade interior maps")
	flag.Parse()
	outputDir := filepath.Join("src", "main", "assets", "maps", "interiors")
	rooms := fixedRooms()
	for _, current := range rooms {
		output := filepath.Join(outputDir, current.file+".tmx")
		if _, err := os.Stat(output); err == nil && !*replace {
			fmt.Fprintf(os.Stderr, "%s already exists; edit it in Tiled or pass -replace to restore the fixed layout\n", output)
			os.Exit(1)
		}
	}
	if err := os.MkdirAll(outputDir, 0o755); err != nil {
		fail(err)
	}
	for _, current := range rooms {
		if err := os.WriteFile(filepath.Join(outputDir, current.file+".tmx"), []byte(buildMap(current)), 0o644); err != nil {
			fail(err)
		}
	}
	fmt.Printf("Wrote %d fixed, editable interiors to %s\n", len(rooms), outputDir)
}

func fixedRooms() []room {
	return []room{
		{
			file: "tavern", title: "THE GOLDEN STAG - COMMON ROOM", theme: "public alehouse",
			width: 12, height: 9, floorGid: 1, wallGid: 6, windowXs: []int{3, 8}, doorX: 5,
			props: []placement{
				{21, 2, 1, 2, 2}, {25, 9, 1, 2, 2}, {18, 9, 4, 1, 1},
				{13, 2, 4, 2, 2}, {13, 6, 4, 2, 2},
				{17, 2, 6, 1, 1}, {17, 7, 6, 1, 1},
				{17, 8, 6, 1, 1}, {17, 9, 6, 1, 1},
				{47, 3, 7, 2, 1}, {47, 7, 7, 2, 1},
			},
		},
		{
			file: "cottage", title: "A HAMLET COTTAGE", theme: "one-room peasant home",
			width: 9, height: 8, floorGid: 49, wallGid: 6, windowXs: []int{2}, doorX: 4,
			props: []placement{
				{9, 1, 1, 2, 2}, {21, 6, 1, 2, 2}, {19, 1, 4, 2, 1},
				{13, 4, 4, 2, 2}, {17, 3, 6, 1, 1}, {65, 7, 5, 1, 1},
			},
		},
		{
			file: "smithy", title: "THE VILLAGE SMITHY", theme: "stone-floored forge",
			width: 10, height: 8, floorGid: 53, wallGid: 6, windowXs: []int{7}, doorX: 4,
			props: []placement{
				{21, 7, 1, 2, 2}, {30, 1, 3, 2, 2}, {29, 5, 4, 1, 1},
				{18, 1, 6, 1, 1}, {83, 7, 4, 2, 1}, {90, 5, 6, 2, 1},
			},
		},
		{
			file: "bakery", title: "THE VILLAGE BAKERY", theme: "stone bakehouse",
			width: 9, height: 8, floorGid: 57, wallGid: 5, windowXs: []int{6}, doorX: 4,
			props: []placement{
				{43, 1, 1, 2, 2}, {25, 6, 1, 2, 2}, {13, 3, 4, 2, 2},
				{70, 1, 5, 2, 2}, {85, 7, 4, 1, 1}, {86, 6, 6, 1, 1},
			},
		},
		{
			file: "chapel", title: "THE VILLAGE CHAPEL", theme: "stone prayer hall",
			width: 10, height: 9, floorGid: 53, wallGid: 5, windowXs: []int{2, 7}, doorX: 4,
			props: []placement{
				{38, 4, 1, 2, 1}, {40, 2, 4, 2, 1}, {40, 6, 4, 2, 1},
				{40, 2, 6, 2, 1}, {40, 6, 6, 2, 1}, {93, 1, 1, 1, 1},
			},
		},
		{
			file: "farmstead", title: "THE FARMSTEAD", theme: "crofter's home",
			width: 10, height: 8, floorGid: 61, wallGid: 6, windowXs: []int{2, 7}, doorX: 4,
			props: []placement{
				{9, 1, 1, 2, 2}, {21, 7, 1, 2, 2}, {13, 4, 4, 2, 2},
				{17, 3, 6, 1, 1}, {74, 1, 5, 2, 2}, {88, 6, 5, 1, 1},
				{87, 7, 5, 1, 1},
			},
		},
		{
			file: "workshop", title: "THE CARPENTER'S WORKSHOP", theme: "carpenter's shed",
			width: 11, height: 8, floorGid: 1, wallGid: 6, windowXs: []int{2, 8}, doorX: 5,
			props: []placement{
				{30, 1, 2, 2, 2}, {19, 8, 1, 2, 1}, {90, 6, 2, 2, 1},
				{83, 1, 5, 2, 1}, {18, 8, 5, 1, 1}, {85, 5, 5, 1, 1},
			},
		},
		{
			file: "weaver", title: "THE WEAVER'S COTTAGE", theme: "loom and spinning wheel",
			width: 9, height: 8, floorGid: 1, wallGid: 6, windowXs: []int{2, 6}, doorX: 4,
			props: []placement{
				{9, 1, 1, 2, 2}, {33, 5, 1, 2, 2}, {66, 1, 4, 2, 2},
				{13, 5, 5, 2, 2}, {65, 3, 1, 1, 1},
			},
		},
		{
			file: "market", title: "THE MARKET STORE", theme: "small village stall",
			width: 9, height: 7, floorGid: 53, wallGid: 5, windowXs: []int{2, 6}, doorX: 4,
			props: []placement{
				{25, 1, 1, 2, 2}, {19, 6, 1, 2, 1}, {70, 1, 4, 2, 2},
				{41, 6, 4, 1, 1}, {18, 6, 5, 1, 1}, {85, 3, 5, 1, 1},
			},
		},
	}
}

func buildMap(current room) string {
	floor := make([]int, current.width*current.height)
	walls := make([]int, current.width*current.height)
	props := make([]int, current.width*current.height)
	for y := 0; y < current.height; y++ {
		for x := 0; x < current.width; x++ {
			index := y*current.width + x
			floor[index] = current.floorGid + (x+y/2)%4
			if x == 0 || x == current.width-1 || y == 0 || y == current.height-1 {
				walls[index] = current.wallGid
			}
		}
	}
	for _, x := range current.windowXs {
		walls[x] = 7
	}
	walls[(current.height-1)*current.width+current.doorX] = 8
	walls[(current.height-1)*current.width+current.doorX+1] = 8
	for _, item := range current.props {
		for y := 0; y < item.height; y++ {
			for x := 0; x < item.width; x++ {
				index := (item.y+y)*current.width + item.x + x
				props[index] = item.tile + y*item.width + x
			}
		}
	}

	var out strings.Builder
	fmt.Fprintf(&out, `<?xml version="1.0" encoding="UTF-8"?>
<map version="1.8" tiledversion="1.8.2" orientation="orthogonal" renderorder="right-down" width="%d" height="%d" tilewidth="16" tileheight="16" infinite="0" backgroundcolor="#65432f" nextlayerid="5" nextobjectid="1">
 <properties><property name="room-title" value="%s"/><property name="room-theme" value="%s"/><property name="entry-x" type="int" value="%d"/><property name="entry-y" type="int" value="%d"/></properties>
 <tileset firstgid="1" name="Hamlet Interior Furniture" tilewidth="16" tileheight="16" tilecount="96" columns="8">
  <image source="../../tilesets/hamlet-interiors.png" width="128" height="192"/>
 </tileset>
`, current.width, current.height, current.title, current.theme, current.doorX, current.height-2)
	writeLayer(&out, 1, "Floor - "+current.theme, current.width, current.height, floor)
	writeLayer(&out, 2, "Walls, windows and door", current.width, current.height, walls)
	writeLayer(&out, 3, "Room furnishings", current.width, current.height, props)
	out.WriteString("</map>\n")
	return out.String()
}

func writeLayer(out *strings.Builder, id int, name string, width, height int, tiles []int) {
	fmt.Fprintf(out, " <layer id=\"%d\" name=\"%s\" width=\"%d\" height=\"%d\">\n  <data encoding=\"csv\">\n", id, name, width, height)
	for index, tile := range tiles {
		if index > 0 {
			if index%width == 0 {
				out.WriteByte('\n')
			} else {
				out.WriteByte(',')
			}
		}
		fmt.Fprint(out, tile)
	}
	out.WriteString("\n  </data>\n </layer>\n")
}

func fail(err error) {
	fmt.Fprintln(os.Stderr, err)
	os.Exit(1)
}
