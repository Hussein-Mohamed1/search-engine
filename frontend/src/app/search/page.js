"use client"
import {redirect, useSearchParams} from "next/navigation";
import {TopBar} from "@/components/topBar";
import useSWR from 'swr'
import Image from "next/image";
import {useEffect, useState} from 'react';
import {ChevronRight} from "lucide-react";

const fetcher = (...args) => fetch(...args).then(res => res.json())

function SearchResults({data}) {


    // Format URL as breadcrumb pieces
    const formatBreadcrumbParts = (url) => {
        try {
            const urlObj = new URL(url);
            const domain = urlObj.hostname;
            const path = urlObj.pathname.split('/').filter(Boolean);
            const queryParams = urlObj.search ? urlObj.search.substring(1).split('&').map(param => param.replace('=', ': ')) : [];

            const parts = [domain, ...path, ...queryParams];
            return parts;
        } catch (e) {
            return [url]; // Return original URL if parsing fails
        }
    };


    return (<div className="flex flex-col gap-6 ml-8">

        {data.map((result, index) => (<div key={index} className="flex flex-col">

            {/* URL and favicon */}
            <div className="flex items-center mb-1">
                <div className="flex items-center">
                    <div
                        className="flex border border-gray-200 rounded-full w-5 h-5 justify-center items-center overflow-hidden mr-2">
                        <Image
                            src="/vercel.svg"
                            alt={result.title}
                            width={12}
                            height={12}
                        />
                    </div>

                    <div className="flex flex-col">
                        <div className="text-[12px] text-white">
                            <span className="font-semibold">{result.url.split('.')[1]}</span>
                        </div>
                        <div className="text-[10px] text-gray-400 flex items-center">
                            {formatBreadcrumbParts(result.url).map((part, idx, arr) => (
                                <span key={idx} className="flex items-center">
                                            {part}
                                    {idx < arr.length - 1 && (<ChevronRight size={8} className="" />)}
                                        </span>))}
                        </div>
                    </div>
                </div>
            </div>

            {/* Title */}
            <a href={result.url}
               className="text-[15px] text-[#75acff] font-medium hover:underline mb-1 leading-tight">
                {result.title}
            </a>

            {/* Description */}
            <div className="text-[10px] text-white leading-snug max-w-[60vw]">
                {result.description}
            </div>
        </div>))}
    </div>);
}

export default function Page() {
    const searchParams = useSearchParams();
    if (!searchParams.has('q') || searchParams.get('q').trim() === "") redirect('/');

    const {data, error, isLoading} = useSWR(`/api/query?${searchParams.toString()}`, fetcher);


    // Add state to track scroll position
    const [scrolled, setScrolled] = useState(false);

    // Add scroll event listener
    useEffect(() => {
        const handleScroll = () => {
            // Set scrolled state based on scroll position
            setScrolled(window.scrollY > 50);
        };

        // Add event listener
        window.addEventListener('scroll', handleScroll);

        // Clean up
        return () => {
            window.removeEventListener('scroll', handleScroll);
        };
    }, []);

    return (<div className="flex flex-col gap-2 min-h-screen">
        {/* Logo and search bar */}
        <div
            className={`sticky top-0 bg-background border-gray-200 z-10 transition-all duration-300 ${scrolled ? 'pt-1' : 'pt-2'}`}>
            <TopBar
                className={`mx-6 items-center transition-all duration-300 ${scrolled ? 'scale-90' : ''}`}
                variant="search"
            />

            <div className={`border-b-[1px] mt-2  transition-all duration-300   ${scrolled ? '-translate-y-1' : ''}`} />

        </div>


        {/* Search results */}

        {isLoading && (<div className="flex justify-center py-8">
            <div className="animate-pulse text-white">Loading results...</div>
        </div>)}

        {error && (<div className="text-red-500">Error loading results. Please try again.</div>)}

        {!isLoading && !error && <SearchResults data={data} />}


    </div>);
}