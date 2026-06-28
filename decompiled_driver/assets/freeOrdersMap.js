const OrderIcon = L.Icon.extend({
  options: {
    iconUrl: 'images/skat-marker.png',
    shadowUrl: 'images/skat-marker-shadow.png',
    iconSize: [52, 52],
    shadowSize: [52, 52],
    iconAnchor: [26, 52],
    shadowAnchor: [0, 52]
  }
})

const orderIcon = new OrderIcon()
const selectedOrderIcon = new OrderIcon({ iconUrl: 'images/skat-marker-selected.png' })


function InitFreeOrdersMap (bridge) {
  var driverMap = new DriverMap()

  driverMap.markers.points = null
  driverMap.markers.selected = null

  driverMap.getMapBounds = function () {
    return driverMap.markers.points.getBounds()
  }

  driverMap.select = function (marker) {
    driverMap.buttons.track.setUserMoved(true)
    driverMap.deSelect()

    marker.setIcon(selectedOrderIcon)
    driverMap.markers.selected = marker

    bridge.callHandler('setActiveOrder', JSON.stringify(driverMap.markers.selected.order))
  }

  driverMap.deSelect = function () {
    if (!driverMap.markers.selected) return
    driverMap.markers.selected.setIcon(orderIcon)
  }

  driverMap.setOrders = function (orders) {
    if (driverMap.markers.points) {
      driverMap.markers.points.clearLayers()
      driverMap.markers.points = null
    }
    const markersGroup = L.markerClusterGroup()

    for (var i = 0; i < orders.length; i++) {
      const order = orders[i];
      if (!order || !order.lat || !order.lon) continue

      const marker = L.marker([parseFloat(order.lat), parseFloat(order.lon)], {
        icon: orderIcon
      })
      marker.order = order
      marker.on('click', function (e) {
        driverMap.select(this)
      })

      markersGroup.addLayer(marker)

      if (driverMap.markers.selected && driverMap.markers.selected.order.oid === marker.order.oid) {
        driverMap.select(marker)
      }
    }

    if (markersGroup.getLayers()) {
      driverMap.markers.points = markersGroup
      driverMap.map.addLayer(driverMap.markers.points)
      driverMap.panToPoints()
    }
  }

  driverMap.initBridgeInterface = function () {
    return {
      resetOrders: (function (data, callback) {
        this.setOrders([])
        callback ? callback() : null
      }).bind(this),
      removeActiveSelection: (function (data, callback) {
        if (this.markers.selected) this.markers.selected.setIcon(orderIcon)
        callback ? callback() : null
      }).bind(this),
      showOrders: (function (data, callback) {
        this.setOrders(JSON.parse(data))
        callback ? callback() : null
      }).bind(this),
    }
  }

  return driverMap
}

// Наша логика после инициализации моста
connectWebViewJavascriptBridge(function (bridge) {
  const driverMap = InitFreeOrdersMap(bridge)

  bridge.init(function(message, callback) {
    document.write(message)
    callback()
  })

  const callbacks = driverMap.getBridgeInterface()
  for (var name in callbacks) {
    bridge.registerHandler(name, callbacks[name])
  }
})
