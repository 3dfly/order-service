#!/usr/bin/env python3
"""
Auto-orient 3D models for optimal 3D printing.
SIMPLIFIED VERSION: Try 6 primary orientations and pick shortest height.
"""

import sys
import trimesh
import numpy as np


def auto_orient_for_printing(input_file, output_file):
    """
    Automatically orient a 3D model for optimal printing.
    Uses simple approach: try 6 orientations, pick the shortest.

    Args:
        input_file: Path to input STL/OBJ/3MF file
        output_file: Path to output file (will be STL)

    Returns:
        dict: Information about the orientation
    """
    try:
        # Load the mesh
        mesh = trimesh.load(input_file)

        # If it's a Scene (e.g., from 3MF), get the combined mesh
        if isinstance(mesh, trimesh.Scene):
            mesh = mesh.dump(concatenate=True)

        # Store original bounds
        original_bounds = mesh.bounds
        original_height = original_bounds[1][2] - original_bounds[0][2]

        # Try 6 primary orientations
        best_height = original_height
        best_transform = np.eye(4)
        best_bottom_area = 0

        test_orientations = [
            ("Original", 0, [1, 0, 0]),
            ("Rotate 90° X", np.pi/2, [1, 0, 0]),
            ("Rotate -90° X", -np.pi/2, [1, 0, 0]),
            ("Rotate 90° Y", np.pi/2, [0, 1, 0]),
            ("Rotate -90° Y", -np.pi/2, [0, 1, 0]),
            ("Rotate 180° X", np.pi, [1, 0, 0]),
        ]

        for name, angle, axis in test_orientations:
            test_mesh = mesh.copy()

            if angle != 0:
                rotation_matrix = trimesh.transformations.rotation_matrix(angle, axis)
                test_mesh.apply_transform(rotation_matrix)

            bounds = test_mesh.bounds
            height = bounds[1][2] - bounds[0][2]

            # Calculate bottom area
            test_normals = test_mesh.face_normals
            bottom_faces_mask = test_normals[:, 2] < -0.8
            bottom_area = np.sum(test_mesh.area_faces[bottom_faces_mask])

            # Prefer shorter height, with tie-breaker for larger bottom area
            is_better = (height < best_height * 0.98) or \
                       (abs(height - best_height) < best_height * 0.02 and bottom_area > best_bottom_area)

            if is_better:
                best_height = height
                best_bottom_area = bottom_area
                best_transform = rotation_matrix if angle != 0 else np.eye(4)

        # Apply best transformation
        if not np.allclose(best_transform, np.eye(4)):
            mesh.apply_transform(best_transform)

        # Center the mesh on the XY plane and place on Z=0
        bounds = mesh.bounds
        translation = np.eye(4)
        translation[0:3, 3] = [
            -(bounds[0][0] + bounds[1][0]) / 2,  # Center X
            -(bounds[0][1] + bounds[1][1]) / 2,  # Center Y
            -bounds[0][2]  # Move bottom to Z=0
        ]
        mesh.apply_transform(translation)

        # Export the oriented mesh
        mesh.export(output_file)

        # Calculate final statistics
        final_bounds = mesh.bounds
        final_height = final_bounds[1][2] - final_bounds[0][2]

        # Find bottom area
        bottom_faces_mask = mesh.face_normals[:, 2] < -0.8
        bottom_area = np.sum(mesh.area_faces[bottom_faces_mask])

        return {
            'success': True,
            'original_height': float(original_height),
            'final_height': float(final_height),
            'height_reduction': float(original_height - final_height),
            'bottom_surface_area': float(bottom_area),
            'bottom_area_percentage': float(bottom_area / mesh.area * 100),
            'rotation_applied': not np.allclose(best_transform, np.eye(4))
        }

    except Exception as e:
        return {
            'success': False,
            'error': str(e)
        }


if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: auto_orient_model.py <input_file> <output_file>")
        print("Example: auto_orient_model.py model.stl model_oriented.stl")
        sys.exit(1)

    input_file = sys.argv[1]
    output_file = sys.argv[2]

    print(f"Auto-orienting {input_file}...")
    result = auto_orient_for_printing(input_file, output_file)

    if result['success']:
        print("✅ Success!")
        print(f"   Original height: {result['original_height']:.2f} mm")
        print(f"   Final height: {result['final_height']:.2f} mm")
        print(f"   Height reduction: {result['height_reduction']:.2f} mm")
        print(f"   Bottom surface area: {result['bottom_surface_area']:.2f} mm²")
        print(f"   Bottom area: {result['bottom_area_percentage']:.1f}% of total")
        print(f"   Rotation applied: {result['rotation_applied']}")
        print(f"   Output saved to: {output_file}")
    else:
        print(f"❌ Error: {result['error']}")
        sys.exit(1)
