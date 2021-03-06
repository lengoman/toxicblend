#!/usr/bin/python   
import bpy
import toxicblend
import imp # needed when reloading toxicblend site-packages, won't be used in a release version

# How to install this plugin:
# 
# run this in the blender console:
#   import site; site.getsitepackages()
#
# copy the content of the toxicblend/src/main/blender_addon/site-packages directory to one of the 
# directories listed by the previous command. 
# 
# OSX example:
# cp -R toxicblend/src/main/blender_addon/site-packages/* /Applications/Blender-2.72b/blender-2.72b.app/Contents/MacOS/../Resources/2.72/python/lib/python3.4/site-packages
#
# then restart blender and use "Run script" on this file

bl_info = {
  "name": "Toxicblend - Median axis",
  'description': 'Naive implementation of median axis using boost voronoi.',
  'author': 'EAD Fritz',
  'blender': (2, 69, 0),
  "category": "Object",
}
       
class ToxicBlend_MedianAxis(bpy.types.Operator):
  '''Naive implementation of median axis'''
  bl_idname = "object.toxicblend_medianaxis"
  bl_label = "Toxicblend:Median axis"
  bl_options = {'REGISTER', 'UNDO'}  # enable undo for the operator.
  
  projectionPlaneProperty = bpy.props.EnumProperty(
    name="Choose 2D plane projection",
    description = "For now manual projection selection will be used.",
    items=(("YZ_PLANE", "YZ",""),
           ("XZ_PLANE", "XZ",""), 
           ("XY_PLANE", "XY","")),
           default="XY_PLANE"    
          )
  useMultiThreadingProperty = bpy.props.EnumProperty(
    description="Each continous ring segment will be processed in a separate thread",
    name="Use mulithreading algorithm",
    items=(("TRUE", "True",""),
           ("FALSE", "False","")),
           default="FALSE"    
          )
          
  #simplifyLimitProperty = bpy.props.FloatProperty(name="Simplify Limit (Not used yet)", default=0.5, min=0.0001, max=100, description="the maximum allowed 3d deviation (in pixels) from a straight line, if the deviation is larger than this the line will be segmented.")  
  zEpsilonProperty = bpy.props.FloatProperty(name="z Epsilon", description="Z values smaller than this is considered to be zero, these points enables 'dot product limit'", default=1.5, min=0.00001, max=10)
  dotProductLimitProperty = bpy.props.FloatProperty(name="Dot Product Limit", description="filter for internal edges relative to the outer ring segment, ideally only edges with 90 degree angles should be kept", default=0.5, min=0.0001, max=1)
  calculationResolutionProperty = 46338 # sqrt(Int.MaxValue)-2
  
  @classmethod
  def poll(cls, context):
    return context.active_object is not None

  def execute(self, context):
    imp.reload(toxicblend)
    try:
      with toxicblend.ByteCommunicator("localhost", 9999) as bc:
        unitSystemProperty = context.scene.unit_settings
        activeObject = context.scene.objects.active
        properties = {'projectionPlane'       : str(self.projectionPlaneProperty), 
                      'useMultiThreading'     : str(self.useMultiThreadingProperty),
                      #'simplifyLimit'         : str(self.simplifyLimitProperty),
                      'zEpsilon'              : str(self.zEpsilonProperty),
                      'dotProductLimit'       : str(self.dotProductLimitProperty),
                      'calculationResolution' : str(self.calculationResolutionProperty),
                      'unitSystem'            : str(unitSystemProperty.system), 
                      'unitScale'             : str(unitSystemProperty.scale_length) }
                       
        bc.sendSingleBlenderObject(activeObject, self.bl_idname, properties) 
        bc.receiveObjects()
        return {'FINISHED'}
    except toxicblend.ToxicblendException as e:
      self.report({'ERROR'}, e.message)
      return {'CANCELLED'}
  
def register():
  bpy.utils.register_class(ToxicBlend_MedianAxis)

def unregister():
  bpy.utils.unregister_class(ToxicBlend_MedianAxis)

if __name__ == "__main__":
  register()
  