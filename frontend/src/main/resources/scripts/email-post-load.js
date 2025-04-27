/**
 * JavaScript for enhancing email display in WebView after content is loaded
 */

// Ensure images don't overflow their containers and improve quality
var images = document.getElementsByTagName('img');
for (var i = 0; i < images.length; i++) {
  images[i].style.maxWidth = '100%';
  images[i].style.height = 'auto';
  // Apply high-quality image rendering
  images[i].style.imageRendering = 'auto';
  images[i].style.msInterpolationMode = 'bicubic';
  // Force smoother rendering by adding a small transform
  images[i].style.transform = 'translateZ(0)';

  // Check if image has width and height attributes
  if (images[i].hasAttribute('width') && images[i].hasAttribute('height')) {
    var width = images[i].getAttribute('width');
    if (width && width.indexOf('%') === -1) {
      // Preserve aspect ratio but don't let it exceed container
      images[i].style.width = 'auto';
      images[i].style.maxWidth = width + 'px';
    }
  }
}

// Fix marketing emails - remove borders from tables
var tables = document.getElementsByTagName('table');
for (var i = 0; i < tables.length; i++) {
  tables[i].style.border = 'none';
  var cells = tables[i].getElementsByTagName('td');
  for (var j = 0; j < cells.length; j++) {
    cells[j].style.border = 'none';
  }
}

// Preserve original backgrounds
var elements = document.querySelectorAll('[bgcolor]');
for (var i = 0; i < elements.length; i++) {
  var bgColor = elements[i].getAttribute('bgcolor');
  if (bgColor) {
    elements[i].style.backgroundColor = bgColor;
  }
}

// Force break long words in emails to prevent horizontal scrolling
document.body.style.wordWrap = 'break-word';
document.body.style.overflowWrap = 'break-word';

// Remove fixed positioning which can cause overlapping content
var allElements = document.getElementsByTagName('*');
for (var i = 0; i < allElements.length; i++) {
  var element = allElements[i];
  var position = window.getComputedStyle(element).getPropertyValue('position');
  if (position === 'fixed') {
    element.style.position = 'static';
  }
}

// Preserve table widths
var widthTables = document.querySelectorAll('table[width]');
for (var i = 0; i < widthTables.length; i++) {
  var width = widthTables[i].getAttribute('width');
  widthTables[i].style.width = width + (width.endsWith('%') ? '' : 'px');
}

// Add smoother scrolling
document.body.style.scrollBehavior = 'smooth';

// Set overflow handling
document.body.style.overflowX = 'hidden'; // Prevent horizontal scrolling
document.body.style.overflowY = 'auto';   // Allow vertical scrolling

// Optimize scrolling performance
document.body.style.willChange = 'scroll-position';
document.body.style.backfaceVisibility = 'hidden';
document.body.style.WebkitFontSmoothing = 'antialiased';
document.body.style.perspective = '1000px';

// Add passive scroll event listener for better performance
window.addEventListener('scroll', function() {
  // This is intentionally empty - the passive property improves scrolling performance
}, { passive: true });

// Optimize large elements for scrolling
var largeElements = document.querySelectorAll('table, div, img');
for (var i = 0; i < largeElements.length; i++) {
  if (largeElements[i].offsetHeight > 100 || largeElements[i].offsetWidth > 100) {
    largeElements[i].style.willChange = 'transform';
    largeElements[i].style.transform = 'translateZ(0)';
  }
}

// Precompute layout for better scrolling
setTimeout(function() {
  window.scrollBy(0, 1);
  window.scrollBy(0, -1);
}, 100);
