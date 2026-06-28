const DEFAULT_CENTER = [55.7558, 37.6173]
const DEFAULT_ZOOM_LEVEL = 10
const MAX_ZOOM_LEVEL = 18

const DRIVER_PAN_ZOOM_LEVEL = 15

function createVehicleMarker (latitude, longitude) {
  const VehicleIcon = L.Icon.extend({
    options: {
      iconUrl: 'images/vehicle.png',
      iconSize: [29, 52],
      iconAnchor: [14, 26]
    }
  })
  vehicleIcon = new VehicleIcon()
  return L.marker([latitude, longitude], { 
    className: 'skat-vehicle',
    icon: vehicleIcon
  })
}

function createTrackButton (driverMap) {
  const TrackControl = L.Control.extend({
    options: {
      position: 'bottomright'
    },
    onAdd: function (map) {
      this.container = L.DomUtil.create('div', 'leaflet-bar leaflet-control track-button')
      this.container.classList.add('track-button')
      this.container.innerHTML = '<a href="">&nbsp;</a>'
      this.container.onclick = (function (e) {
        e.preventDefault()

        this.buttonActivated = !this.buttonActivated
        this.userMoved = false
        this.checkPinned()
        const vehicleLatLng = driverMap.markers.vehicle && driverMap.markers.vehicle.getLatLng()
        if (this.isPinned() && vehicleLatLng) {
          driverMap.map.setView(vehicleLatLng, DRIVER_PAN_ZOOM_LEVEL)
        } else {
          driverMap.mapFitBounds()
        }
        return false
      }).bind(this)
      return this.container
    }
  })

  const trackControl = new TrackControl()

  trackControl.setUserMoved = (function (userMoved) {
    this.userMoved = userMoved
    this.checkPinned()
  }).bind(trackControl)

  trackControl.setButtonActivated = (function (buttonActivated) {
    this.buttonActivated = buttonActivated
    this.checkPinned()
  }).bind(trackControl)

  trackControl.checkPinned = (function () {
    const isPinned = this.isPinned()

    var vehicleLatLng = driverMap.markers.vehicle && driverMap.markers.vehicle.getLatLng()
    if (isPinned && vehicleLatLng) {
      this.container.classList.add('pushed')
    } else {
      this.container.classList.remove('pushed')
    }
  }).bind(trackControl)

  trackControl.isPinned = (function () {
    return this.buttonActivated && !this.userMoved
  }).bind(trackControl)

  return trackControl
}

function DriverMap () {
  this.markers = {
    vehicle: null,
    waypoints: []
  }

  this.buttons = {
      track: null
  }

  // constructor
  this.map = L.map('map').fitWorld()
  window.mPointer = this.map // disableHref.js
  L.tileLayer('http://geo.cloudtaxi.ru/osm_tiles/{z}/{x}/{y}.png', {
    attribution: 'Skat &copy; 2022',
    maxZoom: MAX_ZOOM_LEVEL,
    id: 'mapbox.streets'
  }).addTo(this.map)
  this.map.setView(DEFAULT_CENTER, DEFAULT_ZOOM_LEVEL)

  this.map.on('dragstart', (function (e) {
    if (e.hard) {
      // moved by bounds
    } else { // moved by drag/keyboard
      this.buttons.track.setUserMoved(true)
    }
  }).bind(this))

  // Инициализация кнопок карты
  this.buttons.track = createTrackButton(this)
  this.map.addControl(this.buttons.track)
  // \\ constructor

  this.mapFitBounds = (function () {
    const bounds = L.latLngBounds()
    const childBounds = this.getMapBounds()
    if (childBounds) bounds.extend(childBounds)
    if (!bounds.isValid()) {
      if (this.markers.vehicle) this.map.setView(this.markers.vehicle.getLatLng(), DEFAULT_ZOOM_LEVEL)
      return
    }
    if (this.markers.vehicle) bounds.extend(this.markers.vehicle.getLatLng())
    this.map.fitBounds(bounds, { padding: [50, 50] })
  }).bind(this)

  this.setPosition = function (latitude, longitude, rotation) {
    // Создаем маркер, если он ще не создан (не было координат)
    if (!this.markers.vehicle) {
      this.markers.vehicle = createVehicleMarker(latitude, longitude)
      this.map.addLayer(this.markers.vehicle)
    }

    // Устанавливаем положение маркера
    this.markers.vehicle.setLatLng([latitude, longitude])
    // Поворачиваем маркер в направлении движения
    if (rotation > -1) {
      console.log('===========')
      console.log('bearing', rotation)
      const currentAngle = this.markers.vehicle.getRotationAngle()
      //console.log('current angle', currentAngle)

      var newAngle = rotation
      var difference = currentAngle - newAngle
      //console.log('difference', difference)
      if (difference > 180) newAngle = newAngle + 360
      else if (difference < -180) newAngle = newAngle - 360

      console.log('new angle', newAngle)
      this.markers.vehicle.setRotationAngle(newAngle)
    }

    if (!this.markers.points) {
      this.buttons.track.setButtonActivated(true)
    }
    
    if (!this.buttons.track.userMoved && !this.buttons.track.buttonActivated) {
      this.mapFitBounds()
    } else {
      // Двигаем карту вместе с автомобилем
      if (this.buttons.track.isPinned()) {
        this.map.setView([latitude, longitude], this.map.getZoom(), {
          animate: true,
          pan: { duration: 1, easeLinearity: 1 }
        })
      }
    }
  }

  this.panToPoints = (function () {
    this.buttons.track.setUserMoved(false)
    this.buttons.track.setButtonActivated(false)
    this.mapFitBounds()
  }).bind(this)

  // for children objects
  this.initBridgeInterface = function () {
    return {}
  }
  this.getMapBounds = function () {
    return []
  }

  this.getBridgeInterface = function () {
    const ret = {}
    const currentObj = {
      setPosition: (function (data, callback) {
        var driver = JSON.parse(data)
        this.setPosition(driver.latitude, driver.longitude, driver.rotationAngle)
        callback ? callback() : null
      }).bind(this),
      setCityCenter: (function (data, callback) {
        data = JSON.parse(data)
        const center = new L.LatLng(data.latitude, data.longitude)
        if (this.map.getCenter().equals(DEFAULT_CENTER)) {
            this.map.setView(center, DEFAULT_ZOOM_LEVEL)
        }
        callback ? callback() : null
      }).bind(this),
      fitMapToDriver: (function (data, callback) {
        if (!this.markers.vehicle) return
        this.map.setView(this.markers.vehicle.getLatLng())
        callback ? callback() : null
      }).bind(this)
    }
    const childObj = this.initBridgeInterface()
    Object.keys(currentObj).forEach(function (key) { ret[key] = currentObj[key] })
    Object.keys(childObj).forEach(function (key) { ret[key] = childObj[key] })
    return ret
  }
}

// Инициализация android js-java моста
window.connectWebViewJavascriptBridge = function (callback) {
    if (window.WebViewJavascriptBridge) {
      callback(WebViewJavascriptBridge)
    } else {
      document.addEventListener('WebViewJavascriptBridgeReady', function () {
        callback(WebViewJavascriptBridge)
      }, false)
    }
}

// Растягиваем страницу в полный рост, даже в старых браузерах
function onResize() {
  var map = document.getElementById('map')
  var height = window.innerHeight
  map.style.height = height + 'px'
}

window.onload = window.onresize = onResize
