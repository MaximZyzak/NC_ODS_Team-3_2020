package com.netcracker.odstc.logviewer.containers.converters;

import com.netcracker.odstc.logviewer.containers.AttributeObjectContainer;
import com.netcracker.odstc.logviewer.containers.HierarchyContainer;
import com.netcracker.odstc.logviewer.models.eaventity.EAVObject;
import com.netcracker.odstc.logviewer.models.eaventity.factory.EAVObjectFactory;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AttributeObjectContainerConverter {

    private static final EAVObjectFactory eavObjectFactory = EAVObjectFactory.getInstance();

    public Map<BigInteger, HierarchyContainer> convertAttributeObjectContainerToHierarchyContainer(List<AttributeObjectContainer> attributeObjectContainerList) {

        Map<BigInteger, HierarchyContainer> eavObjectList = new HashMap<>(attributeObjectContainerList.size());
        for (AttributeObjectContainer attributeObjectContainer : attributeObjectContainerList) {

            BigInteger objectId = attributeObjectContainer.getObjectId();
            if (eavObjectList.containsKey(objectId)) {
                appendAttribute(attributeObjectContainer, eavObjectList.get(objectId).getOriginal());
                continue;
            }
            BigInteger objectTypeId = attributeObjectContainer.getObjectTypeId();

            EAVObject eavObject = eavObjectFactory.createEAVObject(objectTypeId);

            eavObject.setParentId(attributeObjectContainer.getParentId());

            eavObject.setObjectTypeId(objectTypeId);
            eavObject.setName(attributeObjectContainer.getName());
            eavObject.setObjectId(objectId);
            eavObjectList.put(objectId, new HierarchyContainer(eavObject));
            appendAttribute(attributeObjectContainer, eavObject);
        }

        setUpHierarchy(eavObjectList);

        return eavObjectList;
    }

    public void setUpHierarchy(Map<BigInteger, HierarchyContainer> eavObjectList) {

        for (Map.Entry<BigInteger, HierarchyContainer> eavObject : eavObjectList.entrySet()) {
            if (eavObjectList.containsKey(eavObject.getValue().getOriginal().getParentId())) {
                HierarchyContainer parent = eavObjectList.get(eavObject.getValue().getOriginal().getParentId());
                eavObject.getValue().setParent(parent);
                parent.addChildren(eavObject.getValue());
            }
        }
        Iterator<Map.Entry<BigInteger, HierarchyContainer>> hierarchyIterator = eavObjectList.entrySet().iterator();
        while (hierarchyIterator.hasNext()) {
            HierarchyContainer hierarchyContainer = hierarchyIterator.next().getValue();
            if (hierarchyContainer.getParent() != null) {
                hierarchyIterator.remove();
            }
        }
    }

    private void appendAttribute(AttributeObjectContainer attributeObjectContainer, EAVObject eavObject) {
        BigInteger attrId = attributeObjectContainer.getAttrId();
        eavObject.setAttributeValue(attrId, attributeObjectContainer.getValue());
        eavObject.setAttributeDateValue(attrId, attributeObjectContainer.getDateValue());
        eavObject.setAttributeListValueId(attrId, attributeObjectContainer.getListValueId());
    }
}
