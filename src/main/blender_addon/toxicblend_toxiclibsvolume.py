#!/usr/bin/python   
import bpy
import toxicblend

# import site; site.getsitepackages()

import imp

bl_info = {
  "name": "Volumetric operation on a lattice built from edges (toxiclibs service)",
  "category": "Object",
}
       
class ToxicLibsVolume(bpy.types.Operator):
  '''Volumetric operation an a lattice'''
  bl_idname = "object.toxicblend_volume"
  bl_label = "Toxiclibs volume"
  bl_options = {'REGISTER', 'UNDO'}  # enable undo for the operator.
  
  voxelBrushType = bpy.props.EnumProperty(
    name="Volumetric brush type",
    items=(("SPHERE", "Round brush",
            "Use a round sphere voxel"),
           ("BOX", "Box brush",
            "Use a box voxel brush")),
           #update=mode_update_callback
           default="SPHERE"    
          )
  voxelBrushMode = bpy.props.EnumProperty(
    name="Volumetric brush mode",
    items=(("MODE_ADDITIVE", "Additive",
           "Use the additive mode"),
           ("MODE_MULTIPLY", "Multiply",
           "Use the multiply mode"),
           ("MODE_REPLACE", "Replace",
           "Use the replace mode"),
           ("MODE_PEAK", "Peak",
           "Use the replace mode. Lower density values don't overwrite existing higher ones")
            #update=mode_update_callback
          ), default="MODE_PEAK")
  voxelBrushSize = bpy.props.FloatProperty(name="Volumetric brush size", default=1, min=0.001, max=25)
  voxelResolution = bpy.props.FloatProperty(name="Voxel resolution", default=32, min=1, max=512)
  voxelIsoValue = bpy.props.FloatProperty(name="Voxel Iso value", default=0.66, min=0.01, max=1.0)
  voxelBrushDrawStep = bpy.props.FloatProperty(name="Voxel brush draw step", default=1, min=0.001, max=256)  
   
  @classmethod
  def poll(cls, context):
    return context.active_object is not None

  def execute(self, context):
    imp.reload(toxicblend)
    with toxicblend.ByteCommunicator("localhost", 9999) as c: 
      # bpy.context.selected_objects,
      activeObject = context.scene.objects.active
      properties = {'voxelBrushSize': str(self.voxelBrushSize), \
                    'voxelResolution': str(self.voxelResolution), \
                    'voxelIsoValue': str(self.voxelIsoValue), \
                    'voxelBrushDrawStep': str(self.voxelBrushDrawStep), \
                    'voxelBrushType': str(self.voxelBrushType),
                    'voxelBrushMode': str(self.voxelBrushMode) }
      #print(str(self.voxelBrushType))               
      c.sendSingleBlenderObject(activeObject, self.bl_idname, properties) 
      c.receiveObjects()
      return {'FINISHED'}

def register():
  bpy.utils.register_class(ToxicLibsVolume)

def unregister():
  bpy.utils.unregister_class(ToxicLibsVolume)

if __name__ == "__main__":
  register()