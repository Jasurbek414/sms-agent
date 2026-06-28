const PLACE_PAN_ZOOM_LEVEL = 14

var createDivIcon = function (text) {
return L.divIcon({
  className: 'skat-marker',
  html: '<div class="skat-marker">' + text + '</div>',
  iconUrl: 'images/skat-marker.png',
  shadowUrl: 'images/skat-marker-shadow.png',
  iconSize: [52, 52],
  shadowSize: [52, 52],
  iconAnchor: [26, 52],
  shadowAnchor: [0, 52]
})
}

function InitRouteMap () {
  var driverMap = new DriverMap()

  driverMap.markers.points = []

  driverMap.clearWaypoints = function () {
    if (driverMap.markers.points) {
      for (var i=0; i<this.markers.points.length; i++) {
        driverMap.map.removeLayer(this.markers.points[i])
      }
    }
    driverMap.markers.points = []
  }

  driverMap.addWaypoint = function (waypoint, label) {
    if (!waypoint.latitude && !waypoint.longitude) return
    const index = driverMap.markers.points.length
    var latLng = [parseFloat(waypoint.latitude), parseFloat(waypoint.longitude)]
    var marker = L.marker(
      latLng,
      { icon: createDivIcon(label) }
    ).on('click', function (e) {
      driverMap.map.setView(latLng, PLACE_PAN_ZOOM_LEVEL)
      return false
    })

    driverMap.map.addLayer(marker)
    driverMap.markers.points.push(marker)
  }

  driverMap.replaceWaypoints = function (waypoints) {
    driverMap.clearWaypoints()
    for (var i=0; i < waypoints.length; i++) {
      var label = i == 0 ? 'A' : (i == waypoints.length-1 ? 'B' : i)
      driverMap.addWaypoint(waypoints[i], label)
    }
    if (waypoints && waypoints[0]) {
      driverMap.panToPoints()
    }
  }

  driverMap.setWaypoints = function (waypoints) {
    if (waypoints == null || waypoints.length != driverMap.markers.points.length) {
      return driverMap.replaceWaypoints(waypoints)
    }

    for (var i=0; i < driverMap.markers.points.length; i++) {
      var oldWaypoint = driverMap.markers.points[i]
      var newWaypoint = waypoints[i]
      var oldLatLng = oldWaypoint.getLatLng()
      var newLatLng = new L.LatLng(parseFloat(newWaypoint.latitude), parseFloat(newWaypoint.longitude))
      if (oldLatLng.equals(newLatLng)) continue

      driverMap.replaceWaypoints(waypoints)
      break
    }
  }

  driverMap.getMapBounds = function () {
    const ret = []
    if (!driverMap.markers.points) return
    for (var i=0; i<driverMap.markers.points.length; i++) {
      ret.push(driverMap.markers.points[i].getLatLng())
    }
    return L.latLngBounds(ret)
  }

  driverMap.initBridgeInterface = function () {
    return {
      focusPlace: (function (data, callback) {
        var params = JSON.parse(data)
        if (this.waypoints.length <= params.index) return
        var waypoint = this.waypoints[params.index]
        this.map.setView(waypoint.getLatLng(), PLACE_PAN_ZOOM_LEVEL)
        callback ? callback() : null
      }).bind(this),
      setWaypoints: (function (data, callback) {
        const waypoints = JSON.parse(data)
        const hasWaypoints = waypoints && waypoints[0]
        this.setWaypoints(waypoints)
        document.title = hasWaypoints ? waypoints[0].street : 'Route Map'
        if (hasWaypoints) setTimeout((function () { this.mapFitBounds() }).bind(this), 1000)
        callback ? callback() : null
      }).bind(this),
      fitMapToWaypoints: (function (data, callback) {
        this.mapFitBounds()
        callback ? callback() : null
      }).bind(this)
    }
  }

  return driverMap
}

// Наша логика после инициализации моста
connectWebViewJavascriptBridge(function (bridge) {
  const driverMap = InitRouteMap(bridge)

  bridge.init(function(message, callback) {
    document.write(message)
    callback()
  })

  const callbacks = driverMap.getBridgeInterface()
  for (var name in callbacks) {
    bridge.registerHandler(name, callbacks[name])
  }
})
