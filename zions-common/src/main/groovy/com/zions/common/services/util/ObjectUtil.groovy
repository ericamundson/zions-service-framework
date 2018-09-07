package com.zions.common.services.util

class ObjectUtil {
	/**
	 * Copy an object structure.
	 * 
	 * @param orig
	 * @return
	 */
	static def deepcopy(orig) {
		def bos = new ByteArrayOutputStream()
		def oos = new ObjectOutputStream(bos)
		oos.writeObject(orig); oos.flush()
		def bin = new ByteArrayInputStream(bos.toByteArray())
		def ois = new ObjectInputStream(bin)
		return ois.readObject()
   }

}
