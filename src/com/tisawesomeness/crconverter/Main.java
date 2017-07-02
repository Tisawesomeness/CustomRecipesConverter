package com.tisawesomeness.crconverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;

public class Main {
	
	public static int counter = 0;
	
	public static void main(String[] args) {
		
		convertFolder(false);
		convertFolder(true);
		System.out.println("Converted " + counter + " recipes!");
		
	}
	
	public static void convertFolder(boolean disabled) {
		
		//Parse disabled variable
		String disabledInput = "./extra_recipes/";
		if (disabled) {
			disabledInput = "./disabled_recipes/";
		}
		String disabledOutput = "";
		if (disabled) {
			disabledOutput = "/disabled";
		}
		
		//Shaped and NBT recipes
		File inputDir = new File(disabledInput + "shaped_recipes");
		if (inputDir.exists() && inputDir.isDirectory()) {
			
			File outputDir = new File("./shaped" + disabledOutput);
			if (!outputDir.exists()) {
				outputDir.mkdir();
			}
			File nbt = new File("./nbt" + disabledOutput);
			if (!nbt.exists()) {
				nbt.mkdir();
			}
			
			for (File f : inputDir.listFiles()) {
				if (f != null && f.isFile()) {
					String name = f.getName();
					System.out.println("Converting " + name);
					convertFile(f, new File(outputDir + "/" + counter + ".dat"), disabled, true);
					counter++;
				}
			}
			
		} else {
			System.out.println(disabledInput + "shaped_recipes folder not found!");
		}
		
		//Shapeless recipes
		inputDir = new File(disabledInput + "shapeless_recipes");
		if (inputDir.exists() && inputDir.isDirectory()) {
			
			File outputDir = new File("./shapeless" + disabledOutput);
			if (!outputDir.exists()) {
				outputDir.mkdir();
			}
			
			for (File f : inputDir.listFiles()) {
				if (f != null && f.isFile()) {
					String name = f.getName();
					System.out.println("Converting " + name);
					convertFile(f, new File(outputDir + "/" + counter + ".dat"), disabled);
					counter++;
				}
			}
			
		} else {
			System.out.println(disabledInput + "shapeless_recipes folder not found!");
		}
		
		//Furnace recipes
		inputDir = new File(disabledInput + "furnace_recipes");
		if (inputDir.exists() && inputDir.isDirectory()) {
			
			File outputDir = new File("./furnace" + disabledOutput);
			if (!outputDir.exists()) {
				outputDir.mkdir();
			}
			
			for (File f : inputDir.listFiles()) {
				if (f != null && f.isFile()) {
					String name = f.getName();
					System.out.println("Converting " + name);
					convertFile(f, new File(outputDir + "/" + counter + ".dat"), disabled);
					counter++;
				}
			}
			
		} else {
			System.out.println(disabledInput + "furnace_recipes folder not found!");
		}
		
	}
	
	public static void convertFile(File inputFile, File outputFile, boolean disabled) {
		convertFile(inputFile, outputFile, disabled, false);
	}
	
	@SuppressWarnings("unchecked")
	public static void convertFile(File inputFile, File outputFile, boolean disabled, boolean checkForNBT) {
		
		//Read from file
		NBTInputStream input;
		try {
			input = new NBTInputStream(new FileInputStream(inputFile), true, ByteOrder.BIG_ENDIAN);
		} catch (IOException ex) {
			System.err.println("Error opening NBT file: " + ex);
			ex.printStackTrace();
			System.exit(1);
			return;
		}
		
		Tag<?> tag;
		try {
			tag = input.readTag();
		} catch (IOException ex) {
			System.err.println("Error reading tag from file: " + ex);
			ex.printStackTrace();
			try {input.close();} catch (IOException ex2) {ex2.printStackTrace();}
			System.exit(1);
			return;
		}
		
		//Get root tag
		CompoundTag root = (CompoundTag) tag;
		CompoundMap map = root.getValue();
		
		//Remove unused experience tag
		boolean furnace = false;
		if (map.containsKey("Experience")) {
			map.remove("Experience");
			furnace = true;
		}
		
		//Make ingredients lowercase
		if (map.containsKey("Ingredients")) {
			ListTag<CompoundTag> ingredients = (ListTag<CompoundTag>) map.get("Ingredients");
			
			//Look for item NBT data and reformat compounds
			List<CompoundTag> items = ingredients.getValue();
			ArrayList<CompoundTag> finalItems = new ArrayList<CompoundTag>();
			for (CompoundTag item : items) {
				if (item.getValue().containsKey("tag") && checkForNBT) {
					if (disabled) {
						outputFile = new File("./nbt/disabled/" + counter + ".dat");
					} else {
						outputFile = new File("./nbt/" + counter + ".dat");
					}
				}
				
				//Put item data in choices list
				CompoundTag choice = new CompoundTag("0", item.getValue());
				List<CompoundTag> choicesArray = new ArrayList<CompoundTag>();
				choicesArray.add(choice);
				ListTag<CompoundTag> choices = new ListTag<CompoundTag>(
					"choices", ingredients.getElementType(), choicesArray);
				CompoundMap choicesMap = new CompoundMap();
				choicesMap.put("choices", choices);
				finalItems.add(new CompoundTag(item.getName(), choicesMap));
			}
			
			ingredients = new ListTag<CompoundTag>("ingredients", ingredients.getElementType(), finalItems);
			map.put("ingredients", ingredients);
			map.remove("Ingredients");
			
		} else {
			CompoundTag ingredient = (CompoundTag) map.get("Ingredient");
			ingredient = new CompoundTag("ingredient", ingredient.getValue());
			map.put("ingredient", ingredient);
			map.remove("Ingredient");
		}
		
		//Make result, height, and width lowercase
		CompoundTag results = (CompoundTag) map.get("Result");
		results = new CompoundTag("result", results.getValue());
		map.put("result", results);
		map.remove("Result");
		
		if (map.containsKey("Height")) {
			IntTag height = (IntTag) map.get("Height");
			height = new IntTag("height", height.getValue());
			map.put("height", height);
			map.remove("Height");
		}
		
		if (map.containsKey("Width")) {
			IntTag width = (IntTag) map.get("Width");
			width = new IntTag("width", width.getValue());
			map.put("width", width);
			map.remove("Width");
		}
		
		//Add key
		if (!furnace) {
			CompoundMap keyMap = new CompoundMap();
			keyMap.put("key", new StringTag("key", String.valueOf(counter)));
			keyMap.put("namespace", new StringTag("namespace", "customrecipes"));
			CompoundTag key = new CompoundTag("key", keyMap);
			map.put("key", key);
		}
		
		//Rebuild root tag
		root = new CompoundTag("", map);
		try {input.close();} catch (IOException ex) {ex.printStackTrace();}
		
		//Write to file
		NBTOutputStream output;
		try {
			output = new NBTOutputStream(new FileOutputStream(outputFile), true, ByteOrder.BIG_ENDIAN);
			output.writeTag(root);
		} catch (IOException ex) {
			System.err.println("Error writing to NBT file: " + ex);
			ex.printStackTrace();
			System.exit(1);
			return;
		}
		
		try {output.close();} catch (IOException ex) {ex.printStackTrace();}
		
	}

}
