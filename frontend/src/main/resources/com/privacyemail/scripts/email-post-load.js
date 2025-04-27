/**
 * JavaScript for enhancing email display in WebView after content is loaded
 */

// Fix tables that are too wide for the viewport
var tables = document.getElementsByTagName('table');
for (var i = 0; i < tables.length; i++) {
  var table = tables[i];
  if (table.offsetWidth > document.body.offsetWidth) {
    table.style.width = '100%';
    table.style.tableLayout = 'fixed';
  }
}

// Ensure images don't overflow their containers
var images = document.getElementsByTagName('img');
for (var i = 0; i < images.length; i++) {
  images[i].style.maxWidth = '100%';
  images[i].style.height = 'auto';
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

// Force all links to open in external browser (handled by Java code)
var links = document.getElementsByTagName('a');
for (var i = 0; i < links.length; i++) {
  links[i].setAttribute('target', '_blank');
  links[i].setAttribute('rel', 'noopener noreferrer');
}

// Add smoother scrolling
document.body.style.scrollBehavior = 'smooth';

// Set overflow handling
document.body.style.overflowX = 'hidden'; // Prevent horizontal scrolling
document.body.style.overflowY = 'auto';   // Allow vertical scrolling
