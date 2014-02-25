#!/usr/bin/python   
import bpy
import toxicblend
import imp

bl_info = {
  "name": "Toxicblend - Custom draw circle operation",
  'description': 'An example on how parameteric geometry can be created in code',
  'author': 'EAD Fritz',
  'blender': (2, 69, 0),
  "category": "Object",
}
       
class ToxicBlend_ParametricCircle(bpy.types.Operator):
  '''Custom draw circle operation'''
  bl_idname = "object.toxicblend_parametriccircleoperation"
  bl_label = "Toxicblend:Custom draw circle"
  bl_options = {'REGISTER', 'UNDO'}  # enable undo for the operator.
  
  projectionPlaneProperty = bpy.props.EnumProperty(
    name="Choose 2D plane projection",
    description = "For now manual projection selection will be used.",
    items=(("YZ_PLANE", "YZ",""),
           ("XZ_PLANE", "XZ",""), 
           ("XY_PLANE", "XY","")),
           #update=mode_update_callback
           default="XY_PLANE"    
          )
  useMultiThreadingProperty = bpy.props.EnumProperty(
    description="Each continous ring segment will be processed in a separate thread",
    name="Use mulithreading algorithm",
    items=(("TRUE", "True",""),
           ("FALSE", "False","")),
           #update=mode_update_callback
           default="FALSE"    
          )
          
  simplifyLimitProperty = bpy.props.FloatProperty(name="Simplify Limit (Not used yet)", default=0.5, min=0.0001, max=100, description="the maximum allowed 3d deviation (in pixels) from a straight line, if the deviation is larger than this the line will be segmented.")  
  zEpsilonProperty = bpy.props.FloatProperty(name="z Epsilon", description="Z values smaller than this is considered to be zero, these points enables 'dot product limit'", default=1.5, min=0.00001, max=10)
  dotProductLimitProperty = bpy.props.FloatProperty(name="Dot Product Limit", description="filter for internal edges relative to the outer ring segment, ideally only edges with 90 degree angles should be kept", default=0.95, min=0.0001, max=1)
  calculationResolutionProperty = 46338 # sqrt(Int.MaxValue)-2

  def execute(self, context):
    imp.reload(toxicblend)
    with toxicblend.ByteCommunicator("localhost", 9999) as c: 
      # bpy.context.selected_objects,
      unitSystemProperty = context.scene.unit_settings
      activeObject = context.scene.objects.active
      properties = {'projectionPlane'       : str(self.projectionPlaneProperty), 
                    'useMultiThreading'     : str(self.useMultiThreadingProperty),
                    'simplifyLimit'         : str(self.simplifyLimitProperty),
                    'zEpsilon'              : str(self.zEpsilonProperty),
                    'dotProductLimit'       : str(self.dotProductLimitProperty),
                    'calculationResolution' : str(self.calculationResolutionProperty),
                    'unitSystem'            : str(unitSystemProperty.system), 
                    'unitScale'             : str(unitSystemProperty.scale_length) }
                     
      c.sendOnlyCommand(self.bl_idname, properties) 
      c.receiveObjects()
      return {'FINISHED'}

def register():
  bpy.utils.register_class(ToxicBlend_ParametricCircle)

def unregister():
  bpy.utils.unregister_class(ToxicBlend_ParametricCircle)

if __name__ == "__main__":
  register()
  