(() => {
  const preferred = (navigator.language || "").toLowerCase();
  let active = preferred.startsWith("de") ? "de" : "en";

  const apply = (lang) => {
    active = lang === "de" ? "de" : "en";
    document.documentElement.lang = active;
    document.querySelectorAll("[data-lang]").forEach((node) => {
      node.hidden = node.dataset.lang !== active;
    });
    document.querySelectorAll("[data-set-lang]").forEach((button) => {
      button.setAttribute("aria-pressed", button.dataset.setLang === active ? "true" : "false");
    });
  };

  document.querySelectorAll("[data-set-lang]").forEach((button) => {
    button.addEventListener("click", () => apply(button.dataset.setLang));
  });
  apply(active);

  // KAL screenshot carousel v2
  const carousel = document.querySelector("[data-carousel]");
  if (carousel) {
    const slides = Array.from(carousel.querySelectorAll("[data-slide]"));
    const dots = Array.from(document.querySelectorAll("[data-dot]"));
    const prev = document.querySelector(".carousel-prev");
    const next = document.querySelector(".carousel-next");
    const lightbox = document.getElementById("screenshot-lightbox");
    const lightboxImage = lightbox?.querySelector("img");
    const zoomLinks = Array.from(carousel.querySelectorAll(".screenshot-zoom"));
    let activeIndex = 0;
    let ticking = false;

    const setActive = (index) => {
      activeIndex = Math.max(0, Math.min(slides.length - 1, index));
      dots.forEach((dot, i) => dot.setAttribute("aria-current", i === activeIndex ? "true" : "false"));
    };

    const nearestIndex = () => {
      const center = carousel.scrollLeft + carousel.clientWidth / 2;
      let best = 0;
      let bestDistance = Infinity;
      slides.forEach((slide, i) => {
        const slideCenter = slide.offsetLeft + slide.offsetWidth / 2;
        const distance = Math.abs(slideCenter - center);
        if (distance < bestDistance) { bestDistance = distance; best = i; }
      });
      return best;
    };

    const goTo = (index) => {
      const nextIndex = (index + slides.length) % slides.length;
      slides[nextIndex].scrollIntoView({behavior: "smooth", block: "nearest", inline: "center"});
      setActive(nextIndex);
    };

    carousel.addEventListener("scroll", () => {
      if (ticking) return;
      ticking = true;
      requestAnimationFrame(() => {
        setActive(nearestIndex());
        ticking = false;
      });
    }, {passive:true});

    carousel.addEventListener("wheel", (event) => {
      if (Math.abs(event.deltaY) <= Math.abs(event.deltaX)) return;
      const atStart = carousel.scrollLeft <= 2;
      const atEnd = carousel.scrollLeft + carousel.clientWidth >= carousel.scrollWidth - 2;
      if ((event.deltaY < 0 && atStart) || (event.deltaY > 0 && atEnd)) return;
      event.preventDefault();
      carousel.scrollBy({left: event.deltaY * 1.15, behavior: "auto"});
    }, {passive:false});

    prev?.addEventListener("click", () => goTo(activeIndex - 1));
    next?.addEventListener("click", () => goTo(activeIndex + 1));
    dots.forEach((dot, i) => dot.addEventListener("click", () => goTo(i)));

    carousel.addEventListener("keydown", (event) => {
      if (event.key === "ArrowRight") { event.preventDefault(); goTo(activeIndex + 1); }
      if (event.key === "ArrowLeft") { event.preventDefault(); goTo(activeIndex - 1); }
    });

    const openLightbox = (index) => {
      if (!lightbox || !lightboxImage) return;
      setActive(index);
      lightboxImage.src = zoomLinks[index].getAttribute("href");
      lightboxImage.alt = slides[index].querySelector("img")?.alt || "KAL launcher screenshot";
      lightbox.hidden = false;
      lightbox.setAttribute("aria-hidden", "false");
      document.body.style.overflow = "hidden";
    };
    const closeLightbox = () => {
      if (!lightbox) return;
      lightbox.hidden = true;
      lightbox.setAttribute("aria-hidden", "true");
      document.body.style.overflow = "";
    };

    zoomLinks.forEach((link, i) => link.addEventListener("click", (event) => {
      event.preventDefault();
      openLightbox(i);
    }));
    lightbox?.querySelector(".lightbox-close")?.addEventListener("click", closeLightbox);
    lightbox?.querySelector(".lightbox-prev")?.addEventListener("click", () => openLightbox((activeIndex - 1 + slides.length) % slides.length));
    lightbox?.querySelector(".lightbox-next")?.addEventListener("click", () => openLightbox((activeIndex + 1) % slides.length));
    lightbox?.addEventListener("click", (event) => { if (event.target === lightbox) closeLightbox(); });
    document.addEventListener("keydown", (event) => {
      if (!lightbox || lightbox.hidden) return;
      if (event.key === "Escape") closeLightbox();
      if (event.key === "ArrowRight") openLightbox((activeIndex + 1) % slides.length);
      if (event.key === "ArrowLeft") openLightbox((activeIndex - 1 + slides.length) % slides.length);
    });

    setActive(0);
  }

})();
