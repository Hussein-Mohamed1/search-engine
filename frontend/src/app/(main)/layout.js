import { Suspense } from "react";
import "../globals.css";

export const metadata = {
  title: "Lumos: Search Engine",
  description:
    "A search engine for the web made as a project for the course Advanced Programming Techniques (APT) at Cairo University.",
};

import { Inter, Nunito, Playwrite_HU, Poppins, Ubuntu } from "next/font/google";

const nunito = Nunito({
  variable: "--font-nunito",
  subsets: ["latin"],
});
const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
});
const poppins = Poppins({
  weight: ["100", "200", "300", "400", "500", "600", "700", "800", "900"],
  variable: "--font-poppins",
  subsets: ["latin"],
});

const playwrite = Playwrite_HU({
  weight: ["100", "200", "300", "400"],
  variable: "--font-poppins",
  subsets: ["latin"],
});

const ubuntu = Ubuntu({
  weight: ["300", "400", "500", "700"],
  variable: "--font-poppins",
  subsets: ["latin"],
});

import { headers } from "next/headers";

export default async function RootLayout({ children }) {
  return (
    <html lang="en">
      <body className={`h-screen w-screen overflow-hidden antialiased`}>
        <Suspense>{children}</Suspense>
      </body>
    </html>
  );
}
