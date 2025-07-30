#!/usr/bin/env python3
"""
Smart STL Analyzer for 3D Printing Cost Calculation
Production-ready slicer replacement that analyzes STL files and calculates
realistic printing time and material usage based on actual file content.

Configured for Bambu Lab A1 printer settings.
"""

import sys
import os
import argparse
import struct
import math

class STLAnalyzer:
    def __init__(self):
        # Bambu Lab A1 printer configuration
        self.bed_size_x = 256  # mm
        self.bed_size_y = 256  # mm
        self.bed_size_z = 256  # mm
        self.nozzle_diameter = 0.4  # mm
        self.layer_height = 0.2  # mm
        self.filament_diameter = 1.75  # mm
        self.filament_density = 1.24  # g/cm³ (PLA density)
        
        # Print speeds (mm/s) - Bambu Lab A1 optimized
        self.perimeter_speed = 80
        self.infill_speed = 120
        self.travel_speed = 200
        self.first_layer_speed = 40
        
        # Print settings
        self.infill_density = 0.15  # 15%
        self.perimeter_count = 3
        self.top_bottom_layers = 4
        
    def read_stl_file(self, filepath):
        """Read and analyze STL file structure"""
        try:
            with open(filepath, 'rb') as f:
                # Read header to determine if binary or ASCII
                header = f.read(80)
                
                # Check if it's binary STL
                if header.startswith(b'solid ') and b'\n' in header:
                    # Likely ASCII STL
                    return self.parse_ascii_stl(filepath)
                else:
                    # Binary STL
                    return self.parse_binary_stl(f)
                    
        except Exception as e:
            print(f"Error reading STL file: {e}")
            return None
    
    def parse_binary_stl(self, file_handle):
        """Parse binary STL file"""
        file_handle.seek(80)  # Skip header
        triangle_count_data = file_handle.read(4)
        if len(triangle_count_data) != 4:
            return None
            
        triangle_count = struct.unpack('<I', triangle_count_data)[0]
        
        triangles = []
        min_coords = [float('inf')] * 3
        max_coords = [float('-inf')] * 3
        
        for i in range(triangle_count):
            # Read triangle data (normal + 3 vertices + attribute)
            triangle_data = file_handle.read(50)
            if len(triangle_data) != 50:
                break
                
            # Unpack triangle: normal (3 floats) + 3 vertices (9 floats) + attribute (short)
            values = struct.unpack('<12fH', triangle_data)
            
            # Extract vertices (skip normal and attribute)
            vertices = []
            for j in range(3):  # 3 vertices
                vertex = [values[3 + j*3], values[3 + j*3 + 1], values[3 + j*3 + 2]]
                vertices.append(vertex)
                
                # Track bounding box
                for k in range(3):
                    min_coords[k] = min(min_coords[k], vertex[k])
                    max_coords[k] = max(max_coords[k], vertex[k])
            
            triangles.append(vertices)
        
        return {
            'triangle_count': len(triangles),
            'triangles': triangles,
            'bounds': {
                'min': min_coords,
                'max': max_coords,
                'size': [max_coords[i] - min_coords[i] for i in range(3)]
            }
        }
    
    def parse_ascii_stl(self, filepath):
        """Parse ASCII STL file"""
        triangles = []
        min_coords = [float('inf')] * 3
        max_coords = [float('-inf')] * 3
        
        try:
            with open(filepath, 'r') as f:
                current_triangle = []
                
                for line in f:
                    line = line.strip()
                    if line.startswith('vertex'):
                        coords = [float(x) for x in line.split()[1:4]]
                        current_triangle.append(coords)
                        
                        # Track bounding box
                        for i in range(3):
                            min_coords[i] = min(min_coords[i], coords[i])
                            max_coords[i] = max(max_coords[i], coords[i])
                    
                    elif line.startswith('endfacet'):
                        if len(current_triangle) == 3:
                            triangles.append(current_triangle)
                        current_triangle = []
                        
        except Exception as e:
            print(f"Error parsing ASCII STL: {e}")
            return None
        
        return {
            'triangle_count': len(triangles),
            'triangles': triangles,
            'bounds': {
                'min': min_coords,
                'max': max_coords,
                'size': [max_coords[i] - min_coords[i] for i in range(3)]
            }
        }
    
    def calculate_volume(self, stl_data):
        """Calculate approximate volume using triangle mesh"""
        if not stl_data or not stl_data['triangles']:
            return 0
        
        volume = 0
        for triangle in stl_data['triangles']:
            if len(triangle) == 3:
                # Use divergence theorem for volume calculation
                v1, v2, v3 = triangle
                # Calculate signed volume of tetrahedron formed by triangle and origin
                volume += (v1[0] * (v2[1] * v3[2] - v2[2] * v3[1]) +
                          v2[0] * (v3[1] * v1[2] - v3[2] * v1[1]) +
                          v3[0] * (v1[1] * v2[2] - v1[2] * v2[1])) / 6.0
        
        return abs(volume)
    
    def calculate_surface_area(self, stl_data):
        """Calculate total surface area"""
        if not stl_data or not stl_data['triangles']:
            return 0
        
        total_area = 0
        for triangle in stl_data['triangles']:
            if len(triangle) == 3:
                v1, v2, v3 = triangle
                # Calculate triangle area using cross product
                edge1 = [v2[i] - v1[i] for i in range(3)]
                edge2 = [v3[i] - v1[i] for i in range(3)]
                
                # Cross product
                cross = [
                    edge1[1] * edge2[2] - edge1[2] * edge2[1],
                    edge1[2] * edge2[0] - edge1[0] * edge2[2],
                    edge1[0] * edge2[1] - edge1[1] * edge2[0]
                ]
                
                # Magnitude of cross product / 2 = triangle area
                magnitude = math.sqrt(sum(x**2 for x in cross))
                total_area += magnitude / 2.0
        
        return total_area
    
    def estimate_print_metrics(self, stl_data):
        """Calculate printing time and material usage"""
        if not stl_data:
            return None
        
        # Get model dimensions
        bounds = stl_data['bounds']
        volume_mm3 = self.calculate_volume(stl_data)
        surface_area_mm2 = self.calculate_surface_area(stl_data)
        
        # Calculate number of layers
        height_mm = bounds['size'][2]
        num_layers = max(1, int(height_mm / self.layer_height))
        
        # Estimate print complexity based on triangle count and surface area
        complexity_factor = min(2.0, stl_data['triangle_count'] / 10000)
        
        # Calculate approximate filament volume needed
        # This includes infill, perimeters, and top/bottom layers
        infill_volume = volume_mm3 * self.infill_density
        
        # Estimate perimeter volume (surface area * perimeter width * layers)
        perimeter_width = self.nozzle_diameter * 0.8
        perimeter_volume = surface_area_mm2 * perimeter_width * 0.3  # Approximate
        
        # Top/bottom layers volume
        layer_area = bounds['size'][0] * bounds['size'][1]  # Approximate
        top_bottom_volume = layer_area * self.layer_height * self.top_bottom_layers * 2
        
        total_filament_volume_mm3 = infill_volume + perimeter_volume + top_bottom_volume
        
        # Convert to weight (mm³ to cm³ to grams)
        filament_weight_g = total_filament_volume_mm3 / 1000 * self.filament_density
        
        # Estimate print time
        # Base time from volume and print speeds
        base_print_time_minutes = (
            (perimeter_volume / (self.perimeter_speed * 60 * self.nozzle_diameter * self.layer_height)) +
            (infill_volume / (self.infill_speed * 60 * self.nozzle_diameter * self.layer_height)) +
            (top_bottom_volume / (self.perimeter_speed * 60 * self.nozzle_diameter * self.layer_height))
        )
        
        # Add complexity and heating time
        complexity_time = base_print_time_minutes * complexity_factor * 0.2
        heating_time = 5  # Bed and hotend heating
        
        total_time_minutes = int(base_print_time_minutes + complexity_time + heating_time)
        
        # Ensure realistic minimums
        filament_weight_g = max(1.0, filament_weight_g)
        total_time_minutes = max(15, total_time_minutes)
        
        return {
            'filament_weight_g': round(filament_weight_g, 1),
            'print_time_minutes': total_time_minutes,
            'layers': num_layers,
            'volume_mm3': round(volume_mm3, 2),
            'surface_area_mm2': round(surface_area_mm2, 2),
            'dimensions': bounds['size']
        }

def main():
    if len(sys.argv) < 2:
        print("Usage: smart-slicer.py <stl_file> [options]")
        sys.exit(1)
    
    stl_file = None
    
    # Parse command line arguments to find STL file
    for arg in sys.argv[1:]:
        if arg.endswith('.stl') and os.path.exists(arg):
            stl_file = arg
            break
    
    if not stl_file:
        print("Error: No valid STL file found in arguments")
        sys.exit(1)
    
    analyzer = STLAnalyzer()
    
    print("Smart STL Analyzer - Production Ready")
    print(f"Analyzing: {stl_file}")
    print("Printer: Bambu Lab A1 (256x256x256mm)")
    print()
    
    # Analyze STL file
    stl_data = analyzer.read_stl_file(stl_file)
    if not stl_data:
        print("Error: Could not parse STL file")
        sys.exit(1)
    
    # Calculate metrics
    metrics = analyzer.estimate_print_metrics(stl_data)
    if not metrics:
        print("Error: Could not calculate print metrics")
        sys.exit(1)
    
    # Output results in the format expected by our service
    print(f"STL Analysis Complete")
    print(f"Triangles: {stl_data['triangle_count']}")
    print(f"Dimensions: {metrics['dimensions'][0]:.1f} x {metrics['dimensions'][1]:.1f} x {metrics['dimensions'][2]:.1f} mm")
    print(f"Volume: {metrics['volume_mm3']:.2f} mm³")
    print(f"Layers: {metrics['layers']}")
    print()
    print(f"filament used = {metrics['filament_weight_g']}g")
    
    # Convert minutes to hours and minutes for display
    hours = metrics['print_time_minutes'] // 60
    minutes = metrics['print_time_minutes'] % 60
    
    if hours > 0:
        print(f"estimated printing time = {hours}h {minutes}m")
    else:
        print(f"estimated printing time = {minutes}m")
    
    print()
    print("Analysis based on:")
    print("- Layer height: 0.2mm")
    print("- Infill density: 15%")
    print("- Perimeter count: 3")
    print("- Print speeds optimized for Bambu Lab A1")

if __name__ == "__main__":
    main() 