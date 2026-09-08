// FrostByte Launcher — shared site behavior
// Vanilla JS, no build step, so the site can be served as-is from GitHub Pages.

(function () {
  "use strict";

  const GITHUB_OWNER = "ProXLegend-YT";
  const GITHUB_REPO = "FrostByteLauncher";
  const RELEASES_API = `https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/releases/latest`;
  const REPO_API = `https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}`;

  // ---------- Scroll reveal ----------
  function initReveal() {
    const targets = document.querySelectorAll("[data-reveal], [data-reveal-group]");
    if (!("IntersectionObserver" in window) || targets.length === 0) {
      targets.forEach((t) => t.classList.add("fb-in"));
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add("fb-in");
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.15, rootMargin: "0px 0px -40px 0px" }
    );
    targets.forEach((t) => observer.observe(t));
  }

  // ---------- Mobile nav ----------
  function initNav() {
    const toggle = document.querySelector(".fb-nav-toggle");
    const links = document.querySelector(".fb-nav-links");
    if (!toggle || !links) return;
    toggle.addEventListener("click", () => {
      const isOpen = links.classList.toggle("fb-nav-links-open");
      toggle.setAttribute("aria-expanded", String(isOpen));
    });
  }

  // ---------- Animated counters ----------
  function initCounters() {
    const counters = document.querySelectorAll("[data-count-to]");
    if (counters.length === 0) return;
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return;
          observer.unobserve(entry.target);
          const el = entry.target;
          const target = parseFloat(el.getAttribute("data-count-to"));
          const suffix = el.getAttribute("data-count-suffix") || "";
          const duration = 1400;
          const start = performance.now();
          function tick(now) {
            const progress = Math.min((now - start) / duration, 1);
            const eased = 1 - Math.pow(1 - progress, 3);
            el.textContent = Math.round(target * eased).toLocaleString() + suffix;
            if (progress < 1) requestAnimationFrame(tick);
          }
          requestAnimationFrame(tick);
        });
      },
      { threshold: 0.5 }
    );
    counters.forEach((c) => observer.observe(c));
  }

  // ---------- GitHub Releases: auto-updating download info ----------
  // Reads the repo's actual latest published Release via the public API — no manual step on
  // your end after a release is tagged and published by the CI workflow.
  async function fetchLatestRelease() {
    const cacheKey = "fb_latest_release_cache_v1";
    const cacheTtlMs = 5 * 60 * 1000;

    try {
      const cached = sessionStorage.getItem(cacheKey);
      if (cached) {
        const parsed = JSON.parse(cached);
        if (Date.now() - parsed.fetchedAt < cacheTtlMs) return parsed.data;
      }
    } catch (e) { /* sessionStorage unavailable or corrupt cache — fall through to network */ }

    const response = await fetch(RELEASES_API, {
      headers: { Accept: "application/vnd.github+json" },
    });
    if (!response.ok) throw new Error(`GitHub API responded ${response.status}`);
    const data = await response.json();

    try {
      sessionStorage.setItem(cacheKey, JSON.stringify({ fetchedAt: Date.now(), data }));
    } catch (e) { /* storage full/unavailable — non-fatal, just skip caching */ }

    return data;
  }

  function formatBytes(bytes) {
    if (!bytes && bytes !== 0) return "";
    const units = ["B", "KB", "MB", "GB"];
    let value = bytes;
    let unitIndex = 0;
    while (value >= 1024 && unitIndex < units.length - 1) {
      value /= 1024;
      unitIndex++;
    }
    return `${value.toFixed(1)} ${units[unitIndex]}`;
  }

  function findApkAsset(release) {
    if (!release || !Array.isArray(release.assets)) return null;
    return (
      release.assets.find((a) => /\.apk$/i.test(a.name) && /release/i.test(a.name)) ||
      release.assets.find((a) => /\.apk$/i.test(a.name)) ||
      null
    );
  }

  async function populateDownloadWidgets() {
    const widgets = document.querySelectorAll("[data-download-widget]");
    if (widgets.length === 0) return;

    widgets.forEach((w) => w.setAttribute("data-state", "loading"));

    try {
      const release = await fetchLatestRelease();
      const asset = findApkAsset(release);

      widgets.forEach((widget) => {
        const button = widget.querySelector("[data-download-button]");
        const versionEl = widget.querySelector("[data-download-version]");
        const sizeEl = widget.querySelector("[data-download-size]");
        const dateEl = widget.querySelector("[data-download-date]");

        if (asset && button) {
          button.href = asset.browser_download_url;
          button.removeAttribute("aria-disabled");
          widget.setAttribute("data-state", "ready");
        } else if (button) {
          // No release published yet, or no matching asset — fall back to the repo's
          // Actions page rather than a dead link, and say so plainly instead of pretending.
          button.href = `https://github.com/${GITHUB_OWNER}/${GITHUB_REPO}/releases`;
          widget.setAttribute("data-state", "no-release");
        }

        if (versionEl) versionEl.textContent = release && release.tag_name ? release.tag_name : "—";
        if (sizeEl) sizeEl.textContent = asset ? formatBytes(asset.size) : "";
        if (dateEl && release && release.published_at) {
          const d = new Date(release.published_at);
          dateEl.textContent = d.toLocaleDateString(undefined, { year: "numeric", month: "long", day: "numeric" });
        }
      });
    } catch (err) {
      widgets.forEach((widget) => {
        widget.setAttribute("data-state", "error");
        const button = widget.querySelector("[data-download-button]");
        if (button) button.href = `https://github.com/${GITHUB_OWNER}/${GITHUB_REPO}/releases`;
      });
      console.warn("FrostByte site: could not fetch latest release —", err.message);
    }
  }

  // ---------- Live repo stats (stars, forks) ----------
  async function populateRepoStats() {
    const els = document.querySelectorAll("[data-repo-stat]");
    if (els.length === 0) return;
    try {
      const response = await fetch(REPO_API, { headers: { Accept: "application/vnd.github+json" } });
      if (!response.ok) throw new Error(`GitHub API responded ${response.status}`);
      const data = await response.json();
      els.forEach((el) => {
        const key = el.getAttribute("data-repo-stat");
        const value = data[key];
        if (typeof value === "number") {
          el.setAttribute("data-count-to", String(value));
        } else if (typeof value === "string") {
          el.textContent = value;
        }
      });
      initCounters();
    } catch (err) {
      console.warn("FrostByte site: could not fetch repo stats —", err.message);
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    initReveal();
    initNav();
    initCounters();
    populateDownloadWidgets();
    populateRepoStats();
  });
})();
